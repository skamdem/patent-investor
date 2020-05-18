package org.launchcode.patentinvestor.models;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by kamdem
 */
@Entity
public class StockDetails extends AbstractEntity {

    //@Size(max = 300, message = "Company name too long!")
    private String companyName;
    private String lastTradeTime;
    private String exchange;
    private String lastUsptoApiUpdate;

    @Column(columnDefinition = "integer default 0")
    private long totalNumberOfPatents = 0;

    @Column(columnDefinition = "double default 0.0")
    private double latestPrice = 0.0;

    public StockDetails(
            String companyName,
            String lastTradeTime,
            String exchange,
            String lastUsptoApiUpdate,
            long totalNumberOfPatents,
            double latestPrice) {
        this.companyName = companyName;
        this.lastTradeTime = lastTradeTime;
        this.exchange = exchange;
        this.lastUsptoApiUpdate = lastUsptoApiUpdate;
        this.totalNumberOfPatents = totalNumberOfPatents;
        this.latestPrice = latestPrice;
    }

    public StockDetails(StockDetails stockDetails) {
        this.companyName = stockDetails.getCompanyName();
        this.lastTradeTime = stockDetails.getLastTradeTime();
        this.exchange = stockDetails.getExchange();
        this.lastUsptoApiUpdate = stockDetails.getLastUsptoApiUpdate();
        this.totalNumberOfPatents = stockDetails.getTotalNumberOfPatents();
        this.latestPrice = stockDetails.getLatestPrice();
    }

    public StockDetails() {
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLastTradeTime() {
        return lastTradeTime;
    }

    public void setLastTradeTime(String lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getLastUsptoApiUpdate() {
        return lastUsptoApiUpdate;
    }

    public void setLastUsptoApiUpdate(String lastUsptoApiUpdate) {
        this.lastUsptoApiUpdate = lastUsptoApiUpdate;
    }

    public long getTotalNumberOfPatents() {
        return totalNumberOfPatents;
    }

    public void setTotalNumberOfPatents(long totalNumberOfPatents) {
        this.totalNumberOfPatents = totalNumberOfPatents;
    }

    public double getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(double latestPrice) {
        this.latestPrice = latestPrice;
    }

}
