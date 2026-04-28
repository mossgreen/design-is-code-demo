package com.disc.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLineSubtotalCalculatorTest {

    private final LineSubtotalCalculator lineSubtotalCalculator = new DefaultLineSubtotalCalculator();

    @Test
    void shouldCalculateOneAtHundred() {
        BigDecimal result = lineSubtotalCalculator.calculate(1, new BigDecimal("100.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldCalculateTwoAtFortyNineNinetyNine() {
        BigDecimal result = lineSubtotalCalculator.calculate(2, new BigDecimal("49.99"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("99.98"));
    }

    @Test
    void shouldCalculateThreeAtTwenty() {
        BigDecimal result = lineSubtotalCalculator.calculate(3, new BigDecimal("20.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void shouldCalculateZeroQuantity() {
        BigDecimal result = lineSubtotalCalculator.calculate(0, new BigDecimal("100.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldRoundHalfUp() {
        BigDecimal result = lineSubtotalCalculator.calculate(3, new BigDecimal("0.333"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> lineSubtotalCalculator.calculate(-1, new BigDecimal("100.00")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
