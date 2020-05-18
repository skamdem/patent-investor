package org.launchcode.patentinvestor.models;

/**
 * Created by kamdem
 */
//DELETE THIS CLASS LATER
public enum ForeignCode {
    FOREIGN("Foreign Corporation", 1), //3
    DOMESTIC("Domestic Corporation", 0); //2

    private final String displayName;
    private final int usptoCode;

    ForeignCode(String displayName, int usptoCode) {
        this.displayName = displayName;
        this.usptoCode = usptoCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getUsptoCode() {
        return usptoCode;
    }
}
