package org.launchcode.patentinvestor.models;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Objects;

/**
 * Created by kamdem
 */
//DELETE THIS CLASS LATER
public class PercentageValue {
    private int percentValue;
    public PercentageValue(int percentValue){
        this.percentValue = percentValue;
    }
    public int getPercentValue() {
        return percentValue;
    }
    public void setPercentValue(int percentValue) {
        this.percentValue = percentValue;
    }
}
