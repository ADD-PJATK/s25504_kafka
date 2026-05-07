package com.s25504.dashboard;

import com.s25504.dashboard.service.StockStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = com.s25504.dashboard.controller.DashboardController.class)
class DashboardControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    StockStreamService stockStreamService;

    @Test
    void tickersEndpoint_returnsJsonFromUpstream() {
        when(stockStreamService.getTickerList())
                .thenReturn(Mono.just("{\"tickers\":[\"ACME\",\"ALFA\"]}"));

        webTestClient.get()
                .uri("/dashboard/tickers")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("ACME");
                    assert body.contains("ALFA");
                });
    }

    @Test
    void streamEndpoint_returnsEventStream() {
        ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                .event("tick")
                .data("{\"ticker\":\"ACME\",\"price\":123.45,\"ts\":\"2026-05-07T10:00:00+02:00\"}")
                .build();

        when(stockStreamService.streamTickers(anyList()))
                .thenReturn(Flux.just(event).delayElements(Duration.ofMillis(10)));

        webTestClient
                .mutate().responseTimeout(Duration.ofSeconds(5)).build()
                .get()
                .uri("/dashboard/stream?tickers=ACME")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .consumeWith(result -> {
                    List<String> body = result.getResponseBody();
                    assert body != null && !body.isEmpty();
                });
    }

    @Test
    void indexEndpoint_servesHtml() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }
}
