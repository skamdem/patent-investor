package org.launchcode.patentinvestor.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kamdem
 */
public class StockData {

    private static ArrayList<String> allCompanyNames = new ArrayList<>();
    private static ArrayList<String> allTickers = new ArrayList<>();

    /**
     * Returns the results of searching the Stocks data by field and search term.
     * For example, searching for ticker "IEX" will include results
     * with "IEX Holdings, Inc".
     *
     * @param column Stock field that should be searched.
     * @param value  Value of the field to search for.
     * @param stockList
     * @return List of all stocks matching the criteria.
     */
    public static ArrayList<Stock> findByColumnAndValue(
            String column,
            String value,
            List<Stock> stockList) {

        ArrayList<Stock> stocks = new ArrayList<>();
        if (value.toLowerCase().equals("all")) {
            return (ArrayList<Stock>) stockList;
        }

        if (column.equals("all")) {
            stocks = findByValue(value, stockList);
            return stocks;
        }
        for (Stock stock : stockList) {
            String aValue = getFieldValue(stock, column);
            if (aValue != null && aValue.toLowerCase().contains(value.toLowerCase())) {
                stocks.add(stock);
            }
        }
        return stocks;
    }

    /**
     * @param stock
     * @param fieldName
     * @return the string value for the corresponding column
     * in that "stock" row
     */
    public static String getFieldValue(Stock stock,
                                       String fieldName) {
        String theValue = "";
        if (fieldName.equals("ticker")) {
            theValue = stock.getTicker();
        } else if (fieldName.equals("companyName")) {
            theValue = stock.getStockDetails().getCompanyName();
        }
        return theValue;
    }

    /**
     * Search all Stock fields for the given term.
     *
     * @param value The search term to look for.
     * @param stockList
     * @return List of all stocks with at least one field containing the value.
     */
    public static ArrayList<Stock> findByValue(String value,
                                               List<Stock> stockList) {
        ArrayList<Stock> stocks = new ArrayList<>();
        for (Stock stock : stockList) {
            if (stock.getTicker().toLowerCase().contains(value.toLowerCase())) {
                stocks.add(stock);
            } else if (stock.getStockDetails().getCompanyName().toLowerCase().contains(value.toLowerCase())) {
                stocks.add(stock);
            }
        }
        return stocks;
    }

}
