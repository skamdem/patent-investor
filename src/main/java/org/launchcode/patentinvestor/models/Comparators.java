package org.launchcode.patentinvestor.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            - s2.getStockDetails().getLatestPrice() < 0) ? -1 : 1;

    public static Comparator<Stock> patentsPortfolioComparator
            = (s1, s2) -> (s1.getStockDetails().getTotalNumberOfPatents()
            - s2.getStockDetails().getTotalNumberOfPatents() < 0) ? -1 : 1;

    //list.sort(tickerComparator.reversed());
    //list.sort(companyNameComparator.reversed());

    public static List<String> paginating(int currentPage, int numberOfPage) {
        int current = currentPage,
                lastPage = numberOfPage,
                delta = 4,
                left = current - delta,
                right = current + delta + 1;
        List<String> range = new ArrayList<>();
        List<String> rangeWithDots = new ArrayList<>();
        int l = 0;

        for (int i = 1; i <= lastPage; i++) {
            if (i == 1 || i == lastPage || i >= left && i < right) {
                range.add("" + i);
            }
        }

        for (String i : range) {
            if (l > 0) {
                if (Integer.parseInt(i) - l == 2) {
                    rangeWithDots.add("" + (l + 1));
                } else if (Integer.parseInt(i) - l != 1) {
                    rangeWithDots.add("...");
                }
            }
            rangeWithDots.add(i);
            l = Integer.parseInt(i);
        }

        return rangeWithDots;
    }
}
