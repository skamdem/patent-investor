package org.launchcode.patentinvestor.models;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kamdem
 */
@Entity
public class Portfolio extends AbstractEntity {

    @OneToMany(mappedBy = "portfolio")
    private final List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio")
    private final List<StockShare> stockShares = new ArrayList<>();

    public Portfolio() {
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<StockShare> getStockShares() {
        return stockShares;
    }

    public List<Stock> getRelevantStocks() {
        Set<Stock> stockSet = new HashSet<Stock>();
        for (StockShare stSh : stockShares) {
            stockSet.add(stSh.getStock());
        }
        return new ArrayList<Stock>(stockSet);
    }

    public List<String> getListOfTickers() {
        List<String> list = new ArrayList<>();
        for (StockShare str : stockShares) {
            list.add(str.getStock().getTicker());
        }
        return list;
    }

    public int getTotalNumberOfShares() {
        int totalNumberOfShares = 0;
        for (StockShare stockShare : stockShares) {
            totalNumberOfShares += stockShare.getNumberOfShares();
        }
        return totalNumberOfShares;
    }

    public int getTotalNumberOfPatents() {
        int totalNumberOfPatents = 0;
        for (StockShare share : stockShares) {
            totalNumberOfPatents += share.getStock().getStockDetails().getTotalNumberOfPatents();
        }
        return totalNumberOfPatents;
    }

    /**
     * getTotalNumberOfShares must be different from zero
     * @return
     */
    public double getAdjustedPatents() {
        double adjustedPatents = 0.0;
        for (StockShare share : stockShares) {
            adjustedPatents += share.getStock().getStockDetails().getTotalNumberOfPatents() * share.getNumberOfShares();
        }
        return adjustedPatents / getTotalNumberOfShares();
    }

    public double getNetWorth() {
        double netWorth = 0.0;
        for (StockShare stockShare : stockShares) {
            netWorth += stockShare.getStock().getStockDetails().getLatestPrice() * stockShare.getNumberOfShares();
        }
        return netWorth;
    }

    public boolean contains(Stock stock) {
        return getListOfTickers().contains(stock.getTicker());
    }

    public int getRelevantNumberOfShares(Stock stock) {
        int numShare = 0;
        for (StockShare stockshare : stockShares) {
            if (stockshare.getStock().equals(stock)) {
                numShare = stockshare.numberOfShares;
                break;
            }
        }
        return numShare;
    }
}
