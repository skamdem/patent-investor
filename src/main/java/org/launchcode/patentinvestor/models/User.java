package org.launchcode.patentinvestor.models;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by kamdem
 */
@Entity
public class User extends AbstractEntity {
    @NotNull
    private String username;

    @NotNull
    private String pwHash;

    @OneToOne(cascade = CascadeType.ALL)
    @Valid //validate objects contained within the object up to their inner fields
    @NotNull
    private Portfolio portfolio;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.pwHash = encoder.encode(password);
        this.portfolio = new Portfolio(Portfolio.DEFAULT_PRICE_IP_RATIO);
    }

    public String getUsername() {
        return username;
    }

    public void setUserRatio(double priceIpRatio){
        this.portfolio.setPriceIpRatio(priceIpRatio);
    }

    public double getUserRatio(){
        return this.portfolio.getPriceIpRatio();
    }


    public boolean isMatchingPassword(String password) {
        return encoder.matches(password, pwHash);
    }
}
