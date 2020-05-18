package org.launchcode.patentinvestor.models;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kamdem
 */
@Entity
public class Tag extends AbstractEntity {
    @Size(min = 2, max = 25, message = "Investment field must be between 1 and 25 characters")
    @NotBlank(message = "name is required")
    private String name;

    @Size(min = 2, max = 300, message = "Description of the investment field must be between 1 and 300 characters")
    @NotBlank(message = "description is required")
    private String description;

    @ManyToMany(mappedBy = "tags")
    private final List<Stock> stocks = new ArrayList<>();

    @ManyToOne
    @NotNull(message = "portfolio is required")
    private Portfolio portfolio;

    public Tag(String name,
               String description,
               Portfolio portfolio) {
        this.name = name;
        this.description = description;
        this.portfolio = portfolio;
    }

    public Tag(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public Tag() {
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return "#" + name + " ";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
}
