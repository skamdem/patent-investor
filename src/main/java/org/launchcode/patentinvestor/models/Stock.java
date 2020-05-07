package org.launchcode.patentinvestor.models;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kamdem
 */
@Entity
public class Stock extends AbstractEntity {

    @NotBlank(message = "Ticker is required")
    @Size(min = 3, max = 10, message = "Ticker must be between 3 and 10 characters")
    private String ticker;

    @OneToOne(cascade = CascadeType.ALL)
    @Valid //validate objects contained within the object up to their inner fields
    @NotNull
    private StockDetails stockDetails;

    @ManyToMany
    @JoinTable(name = "STOCK_TAGS")
    private final List<Tag> tags = new ArrayList<>();

    @NotBlank(message = "usptoId is required")
    private String usptoId;

    @NotBlank(message = "iexId is required")
    private String iexId;

    public Stock(String ticker, String usptoId, String iexIid) {
        this.ticker = ticker;
        this.usptoId = usptoId;
        this.iexId = iexIid;
    }

    public Stock() {
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public StockDetails getStockDetails() {
        return stockDetails;
    }

    public void setStockDetails(StockDetails stockDetails) {
        this.stockDetails = stockDetails;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public String getUsptoId() {
        return usptoId;
    }

    public void setUsptoId(String usptoId) {
        this.usptoId = usptoId;
    }

    public String getIexId() {
        return iexId;
    }

    public void setIexId(String iexId) {
        this.iexId = iexId;
    }

    @Override
    public String toString() {
        return ticker;
    }
}
