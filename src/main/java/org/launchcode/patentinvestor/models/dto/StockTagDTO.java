package org.launchcode.patentinvestor.models.dto;

import org.launchcode.patentinvestor.models.Stock;
import org.launchcode.patentinvestor.models.Tag;

import javax.validation.constraints.NotNull;

/**
 * Created by kamdem
 */
public class StockTagDTO {
    @NotNull
    private Stock stock;

    @NotNull
    private Tag tag;

    public StockTagDTO() {
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
}
