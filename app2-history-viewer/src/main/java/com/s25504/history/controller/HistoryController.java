package com.s25504.history.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.s25504.history.model.StockTick;
import com.s25504.history.service.StockHistoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class HistoryController {

    private final StockHistoryService historyService;
    private final ObjectMapper objectMapper;

    public HistoryController(StockHistoryService historyService, ObjectMapper objectMapper) {
        this.historyService = historyService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Returns the list of tickers currently being tracked by the background poller. */
    @GetMapping("/history/tickers")
    @ResponseBody
    public List<String> trackedTickers() {
        return historyService.getTrackedTickers();
    }

    /**
     * Returns accumulated ticks for the requested tickers within the last N minutes.
     * Example: GET /history/data?tickers=ACME&tickers=ALFA&minutes=5
     */
    @GetMapping("/history/data")
    @ResponseBody
    public Map<String, Object> historyData(
            @RequestParam("tickers") List<String> tickers,
            @RequestParam(value = "minutes", defaultValue = "5") int minutes) {

        List<StockTick> ticks = historyService.getHistory(tickers, minutes);
        return Map.of(
                "tickers", tickers,
                "minutes", minutes,
                "count", ticks.size(),
                "data", ticks
        );
    }

    /**
     * Downloads accumulated ticks as CSV or JSON.
     * Example: GET /history/export?tickers=ACME&minutes=5&format=csv
     */
    @GetMapping("/history/export")
    public ResponseEntity<String> export(
            @RequestParam("tickers") List<String> tickers,
            @RequestParam(value = "minutes", defaultValue = "5") int minutes,
            @RequestParam(value = "format", defaultValue = "csv") String format)
            throws JsonProcessingException {

        List<StockTick> ticks = historyService.getHistory(tickers, minutes);

        if ("json".equalsIgnoreCase(format)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ticks);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=history.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        }

        String csv = historyService.exportAsCsv(ticks);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=history.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
