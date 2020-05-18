package org.launchcode.patentinvestor.models;

import java.util.Comparator;

/**
 * Created by kamdem
 */
public class Comparators {

    public static Comparator<Stock> tickerComparator
            = (s1, s2) -> s1.getTicker().compareTo(s2.getTicker());

    public static Comparator<Tag> tagComparator
            = (t1, t2) -> t1.getName().compareTo(t2.getName());

    public static Comparator<Stock> companyNameComparator
            = (s1, s2) -> s1.getStockDetails().getCompanyName()
            .compareTo(s2.getStockDetails().getCompanyName());

    public static Comparator<Stock> latestPriceComparator
            = (s1, s2) -> (s1.getStockDetails().getLatestPrice()
            - s2.getStockDetails().getLatestPrice() == 0) ? 0 : ((s1.getStockDetails().getLatestPrice()
            - s2.getStockDetails().getLatestPrice() < 0) ? -1 : 1);

    public static Comparator<Stock> patentsPortfolioComparator
            = (s1, s2) -> (s1.getStockDetails().getTotalNumberOfPatents()
            - s2.getStockDetails().getTotalNumberOfPatents() == 0) ? 0 : ((s1.getStockDetails().getTotalNumberOfPatents()
            - s2.getStockDetails().getTotalNumberOfPatents() < 0) ? -1 : 1);

    public static Comparator<StockShare> percentInPortfolioComparator
            = (s1, s2) -> (s1.getNumberOfShares() * s1.getStock().getStockDetails().getLatestPrice()
            - s2.getNumberOfShares() * s2.getStock().getStockDetails().getLatestPrice() == 0) ? 0 : ((s1.getNumberOfShares() * s1.getStock().getStockDetails().getLatestPrice()
            - s2.getNumberOfShares() * s2.getStock().getStockDetails().getLatestPrice() < 0) ? -1 : 1);

    public static Comparator<StockShare> numberOfSharesComparator
            = (s1, s2) -> Integer.compare(s1.getNumberOfShares()
            - s2.getNumberOfShares(), 0);

    //list.sort(tickerComparator.reversed());
    //list.sort(companyNameComparator.reversed());
}
