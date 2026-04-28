package com.disc.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DefaultBulkDiscountCalculator implements BulkDiscountCalculator {

    private static final BigDecimal TIER_2_RATE = new BigDecimal("0.10");
    private static final BigDecimal TIER_3_RATE = new BigDecimal("0.20");
    private static final int TIER_2_THRESHOLD = 5;
    private static final int TIER_3_THRESHOLD = 10;

    @Override
    public BigDecimal calculate(Integer quantity, BigDecimal lineSubtotal) {
        if (quantity == null || lineSubtotal == null) {
            throw new IllegalArgumentException("quantity and lineSubtotal must not be null");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        BigDecimal rate;
        if (quantity >= TIER_3_THRESHOLD) {
            rate = TIER_3_RATE;
        } else if (quantity >= TIER_2_THRESHOLD) {
            rate = TIER_2_RATE;
        } else {
            rate = BigDecimal.ZERO;
        }
        return lineSubtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
