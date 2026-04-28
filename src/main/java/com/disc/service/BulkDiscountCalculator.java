package com.disc.service;

import java.math.BigDecimal;

public interface BulkDiscountCalculator {
    BigDecimal calculate(Integer quantity, BigDecimal lineSubtotal);
}
