package org.launchcode.patentinvestor.models;

import javax.persistence.Entity;

/**
 * Created by kamdem
 */
@Entity
public class Portfolio extends AbstractEntity {

    public static final double DEFAULT_PRICE_IP_RATIO = 0.5;

    private double priceIpRatio;

    public Portfolio(double priceIpRatio) {
        this.priceIpRatio = priceIpRatio;
    }

    public Portfolio() {
    }

    public double getPriceIpRatio() {
        return priceIpRatio;
    }

    public void setPriceIpRatio(double priceIpRatio) {
        this.priceIpRatio = priceIpRatio;
    }
}
