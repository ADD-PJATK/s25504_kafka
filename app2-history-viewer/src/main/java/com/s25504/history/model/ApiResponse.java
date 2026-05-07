package com.s25504.history.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse {

    private List<StockTick> data;
    private List<String> tickers;

    public List<StockTick> getData() { return data; }
    public void setData(List<StockTick> data) { this.data = data; }

    public List<String> getTickers() { return tickers; }
    public void setTickers(List<String> tickers) { this.tickers = tickers; }
}
