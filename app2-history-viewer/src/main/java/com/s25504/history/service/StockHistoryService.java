package com.s25504.history.service;

import com.s25504.history.model.ApiResponse;
import com.s25504.history.model.StockTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StockHistoryService {

    private static final Logger log = LoggerFactory.getLogger(StockHistoryService.class);
    private static final int MAX_TICKS_PER_TICKER = 120;

    private final RestClient restClient;
    private final List<String> trackedTickers;

    /** ticker -> time-ordered deque (newest first) */
    private final Map<String, Deque<StockTick>> store = new ConcurrentHashMap<>();

    public StockHistoryService(
            RestClient restClient,
            @Value("${history.tracked-tickers}") String trackedTickersConfig) {
        this.restClient = restClient;
        this.trackedTickers = Arrays.asList(trackedTickersConfig.split(","));
        trackedTickers.forEach(t -> store.put(t.trim(), new ArrayDeque<>()));
        log.info("Tracking tickers: {}", trackedTickers);
    }

    /** Polls /api/latest for all tracked tickers every 10 seconds. */
    @Scheduled(fixedDelay = 10_000)
    public void pollLatest() {
        if (trackedTickers.isEmpty()) return;

        URI uri = UriComponentsBuilder.fromPath("/api/latest")
                .queryParam("ticker", trackedTickers.toArray())
                .build()
                .toUri();

        try {
            ApiResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(ApiResponse.class);

            if (response == null || response.getData() == null) return;

            response.getData().forEach(tick -> {
                if (tick.getTicker() == null) return;
                Deque<StockTick> deque = store.computeIfAbsent(
                        tick.getTicker(), k -> new ArrayDeque<>());
                synchronized (deque) {
                    // Deduplicate by seq number
                    if (deque.isEmpty() || !Objects.equals(deque.peekFirst().getSeq(), tick.getSeq())) {
                        deque.addFirst(tick);
                        if (deque.size() > MAX_TICKS_PER_TICKER) deque.pollLast();
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Poll failed: {}", e.getMessage());
        }
    }

    public List<String> getTrackedTickers() {
        return Collections.unmodifiableList(trackedTickers);
    }

    /**
     * Returns ticks for the requested tickers within the last {@code minutes} minutes,
     * sorted ascending by timestamp.
     */
    public List<StockTick> getHistory(List<String> tickers, int minutes) {
        Instant cutoff = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        return tickers.stream()
                .flatMap(ticker -> {
                    Deque<StockTick> deque = store.getOrDefault(ticker, new ArrayDeque<>());
                    synchronized (deque) {
                        return new ArrayList<>(deque).stream();
                    }
                })
                .filter(tick -> {
                    try {
                        return OffsetDateTime.parse(tick.getTs()).toInstant().isAfter(cutoff);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(StockTick::getTs))
                .collect(Collectors.toList());
    }

    /** Serialises a list of ticks to CSV string. */
    public String exportAsCsv(List<StockTick> ticks) {
        StringBuilder sb = new StringBuilder("ticker,ts,price,currency,volume,seq\n");
        ticks.forEach(t -> sb.append(String.format("%s,%s,%s,%s,%s,%s\n",
                t.getTicker(),
                t.getTs(),
                t.getPrice(),
                t.getCurrency(),
                t.getVolume(),
                t.getSeq())));
        return sb.toString();
    }
}
