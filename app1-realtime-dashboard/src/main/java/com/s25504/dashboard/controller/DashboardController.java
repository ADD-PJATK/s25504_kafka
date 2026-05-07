package com.s25504.dashboard.controller;

import com.s25504.dashboard.service.StockStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
public class DashboardController {

    private final StockStreamService stockStreamService;

    public DashboardController(StockStreamService stockStreamService) {
        this.stockStreamService = stockStreamService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Returns the full list of 50 tickers as raw JSON from the upstream API. */
    @GetMapping("/dashboard/tickers")
    @ResponseBody
    public Mono<String> tickers() {
        return stockStreamService.getTickerList();
    }

    /**
     * Proxies the upstream SSE stream(s) for the requested tickers.
     * The browser connects here with EventSource; this endpoint merges
     * one upstream SSE connection per ticker into a single stream.
     */
    @GetMapping(value = "/dashboard/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam("tickers") List<String> tickers) {
        return stockStreamService.streamTickers(tickers);
    }
}
