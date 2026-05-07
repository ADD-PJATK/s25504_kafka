package com.s25504.history.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockTick {

    private String ticker;
    private String ts;
    private BigDecimal price;
    private String currency;
    private Integer volume;
    private Integer seq;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getTs() { return ts; }
    public void setTs(String ts) { this.ts = ts; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getVolume() { return volume; }
    public void setVolume(Integer volume) { this.volume = volume; }

    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
}
