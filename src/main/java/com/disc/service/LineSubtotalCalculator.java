package com.disc.service;

import java.math.BigDecimal;

public interface LineSubtotalCalculator {
    BigDecimal calculate(Integer quantity, BigDecimal unitPrice);
}
