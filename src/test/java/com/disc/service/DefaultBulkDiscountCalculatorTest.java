package com.disc.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultBulkDiscountCalculatorTest {

    private final BulkDiscountCalculator bulkDiscountCalculator = new DefaultBulkDiscountCalculator();

    @Test
    void shouldGiveNoDiscountForOneItem() {
        BigDecimal result = bulkDiscountCalculator.calculate(1, new BigDecimal("100.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldGiveNoDiscountForFourItems() {
        BigDecimal result = bulkDiscountCalculator.calculate(4, new BigDecimal("400.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldGiveTenPercentDiscountForFiveItems() {
        BigDecimal result = bulkDiscountCalculator.calculate(5, new BigDecimal("500.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldGiveTenPercentDiscountForFiveItemsAtFortyNineNinetyNine() {
        BigDecimal result = bulkDiscountCalculator.calculate(5, new BigDecimal("49.99"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void shouldGiveTenPercentDiscountForNineItems() {
        BigDecimal result = bulkDiscountCalculator.calculate(9, new BigDecimal("900.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    void shouldGiveTwentyPercentDiscountForTenItems() {
        BigDecimal result = bulkDiscountCalculator.calculate(10, new BigDecimal("1000.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void shouldGiveTwentyPercentDiscountForTwentyItems() {
        BigDecimal result = bulkDiscountCalculator.calculate(20, new BigDecimal("999.80"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("199.96"));
    }

    @Test
    void shouldGiveNoDiscountForZeroQuantity() {
        BigDecimal result = bulkDiscountCalculator.calculate(0, new BigDecimal("0.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> bulkDiscountCalculator.calculate(-1, new BigDecimal("100.00")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
