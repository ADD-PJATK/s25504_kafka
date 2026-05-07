package com.s25504.dashboard.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class StockStreamService {

    private final WebClient webClient;

    public StockStreamService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getTickerList() {
        return webClient.get()
                .uri("/api/tickers")
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Merges per-ticker SSE streams from the upstream API into a single Flux.
     * Each upstream event (event: tick) is re-emitted as a ServerSentEvent to the browser.
     */
    public Flux<ServerSentEvent<String>> streamTickers(List<String> tickers) {
        List<Flux<ServerSentEvent<String>>> streams = tickers.stream()
                .map(ticker -> webClient.get()
                        .uri(u -> u.path("/api/stream").queryParam("ticker", ticker).build())
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                        .retry(5)
                        .onErrorResume(e -> Flux.empty()))
                .toList();

        return Flux.merge(streams);
    }
}
