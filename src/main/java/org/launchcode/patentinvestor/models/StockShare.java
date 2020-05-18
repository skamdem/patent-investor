package org.launchcode.patentinvestor.models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

/**
 * Created by kamdem
 */
@Entity
public class StockShare extends AbstractEntity {

    @ManyToOne
    @NotNull(message = "portfolio is required")
    private Portfolio portfolio;

    @ManyToOne
    @NotNull(message = "stock is required")
    private Stock stock;

    int numberOfShares = 0;

    public StockShare() {
    }

    public StockShare(
            Stock stock,
            Portfolio portfolio) {
        this.portfolio = portfolio;
        this.stock = stock;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public int getNumberOfShares() {
        return numberOfShares;
    }

    public void setNumberOfShares(int numberOfShares) {
        this.numberOfShares = numberOfShares;
    }
}
