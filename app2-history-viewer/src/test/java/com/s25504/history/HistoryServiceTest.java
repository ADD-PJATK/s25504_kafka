package com.s25504.history;

import com.s25504.history.model.StockTick;
import com.s25504.history.service.StockHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HistoryServiceTest {

    private StockHistoryService service;

    @BeforeEach
    void setUp() {
        RestClient mockClient = mock(RestClient.class);
        service = new StockHistoryService(mockClient, "ACME,ALFA");
    }

    /** Injects ticks directly into the service's internal store for unit testing. */
    @SuppressWarnings("unchecked")
    private void injectTick(String ticker, String ts, double price, int seq) throws Exception {
        Field storeField = StockHistoryService.class.getDeclaredField("store");
        storeField.setAccessible(true);
        Map<String, Deque<StockTick>> store = (Map<String, Deque<StockTick>>) storeField.get(service);

        StockTick tick = new StockTick();
        tick.setTicker(ticker);
        tick.setTs(ts);
        tick.setPrice(BigDecimal.valueOf(price));
        tick.setCurrency("PLN");
        tick.setVolume(1000);
        tick.setSeq(seq);

        store.computeIfAbsent(ticker, k -> new ArrayDeque<>()).addFirst(tick);
    }

    @Test
    void getHistory_returnsTicksWithinTimeWindow() throws Exception {
        String recentTs = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2).toString();
        String oldTs    = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(8).toString();

        injectTick("ACME", recentTs, 120.0, 1);
        injectTick("ACME", oldTs,    115.0, 2);

        List<StockTick> result = service.getHistory(List.of("ACME"), 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeq()).isEqualTo(1);
    }

    @Test
    void getHistory_returnsEmptyForUnknownTicker() {
        List<StockTick> result = service.getHistory(List.of("UNKNOWN"), 5);
        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_returnsMultipleTickers() throws Exception {
        String ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).toString();
        injectTick("ACME", ts, 100.0, 10);
        injectTick("ALFA", ts, 200.0, 20);

        List<StockTick> result = service.getHistory(List.of("ACME", "ALFA"), 5);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(StockTick::getTicker))
                .containsExactlyInAnyOrder("ACME", "ALFA");
    }

    @Test
    void exportAsCsv_containsHeaderAndData() throws Exception {
        String ts = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1).toString();
        injectTick("ACME", ts, 123.45, 99);

        List<StockTick> ticks = service.getHistory(List.of("ACME"), 5);
        String csv = service.exportAsCsv(ticks);

        assertThat(csv).startsWith("ticker,ts,price,currency,volume,seq");
        assertThat(csv).contains("ACME");
        assertThat(csv).contains("123.45");
    }

    @Test
    void getTrackedTickers_returnsConfiguredList() {
        List<String> tickers = service.getTrackedTickers();
        assertThat(tickers).containsExactly("ACME", "ALFA");
    }
}
