package com.payments.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// @ConfigurationProperties: application.yml ke fraud rules inject karta hai
@Configuration
@ConfigurationProperties(prefix = "fraud")
public class FraudProperties {

    private BigDecimal maxAmount = new BigDecimal("50000.00");
    private int maxVelocityPerMinute = 5;

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public int getMaxVelocityPerMinute() {
        return maxVelocityPerMinute;
    }

    public void setMaxVelocityPerMinute(int maxVelocityPerMinute) {
        this.maxVelocityPerMinute = maxVelocityPerMinute;
    }
}
