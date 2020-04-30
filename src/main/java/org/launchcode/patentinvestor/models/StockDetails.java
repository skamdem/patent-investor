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
    private ForeignCode foreignCode;
    private String lastUsptoApiUpdate;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean inPortfolio = false;

    @Column(columnDefinition = "integer default 0")
    private int numberOfShares = 0;

    @Column(columnDefinition = "integer default 0")
    private long totalNumberOfPatents = 0;

    @Column(columnDefinition = "double default 0.0")
    private double latestPrice = 0.0;

    public StockDetails(String companyName, String lastTradeTime, String exchange,
                        ForeignCode foreignCode, String lastUsptoApiUpdate, long totalNumberOfPatents,
                        double latestPrice, boolean inPortfolio, int numberOfShares) {
        this.companyName = companyName;
        this.lastTradeTime = lastTradeTime;
        this.exchange = exchange;
        this.foreignCode = foreignCode;
        this.lastUsptoApiUpdate = lastUsptoApiUpdate;
        this.totalNumberOfPatents = totalNumberOfPatents;
        this.latestPrice = latestPrice;
        this.inPortfolio = inPortfolio;
        this.numberOfShares = numberOfShares;
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

    public ForeignCode getForeignCode() {
        return foreignCode;
    }

    public void setForeignCode(ForeignCode foreignCode) {
        this.foreignCode = foreignCode;
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

    public boolean isInPortfolio() {
        return inPortfolio;
    }

    public void setInPortfolio(boolean inPortfolio) {
        this.inPortfolio = inPortfolio;
    }

    public int getNumberOfShares() {
        return numberOfShares;
    }

    public void setNumberOfShares(int numberOfShares) {
        this.numberOfShares = numberOfShares;
    }

}
