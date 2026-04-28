package com.disc.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DefaultLineSubtotalCalculator implements LineSubtotalCalculator {

    @Override
    public BigDecimal calculate(Integer quantity, BigDecimal unitPrice) {
        if (quantity == null || unitPrice == null) {
            throw new IllegalArgumentException("quantity and unitPrice must not be null");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
