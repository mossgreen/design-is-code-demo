package com.disc.service;

import com.disc.entity.Product;
import com.disc.factory.SaleResponseFactory;
import com.disc.model.SaleLineItemRequest;
import com.disc.model.SaleRequest;
import com.disc.model.SaleResponse;
import com.disc.model.SaleResponseWithDiscount;
import com.disc.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultSaleServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private LineSubtotalCalculator lineSubtotalCalculator;
    @Mock private BulkDiscountCalculator bulkDiscountCalculator;
    @Mock private SaleResponseFactory saleResponseFactory;

    @Mock private SaleRequest saleRequest;
    @Mock private SaleResponse saleResponse;
    @Mock private SaleResponseWithDiscount saleResponseWithDiscount;
    @Mock private Product product;

    private final UUID productId = UUID.randomUUID();
    private final Integer quantity = 2;
    private final BigDecimal price = new BigDecimal("10.00");
    private final SaleLineItemRequest lineItem = new SaleLineItemRequest(productId, quantity);
    private final BigDecimal lineSubtotal = new BigDecimal("20.00");
    private final BigDecimal lineDiscount = new BigDecimal("2.00");

    private SaleResponse result;
    private SaleResponseWithDiscount resultWithDiscount;

    DefaultSaleService defaultSaleService;

    @BeforeEach
    void setUp() {
        defaultSaleService = new DefaultSaleService(productRepository, lineSubtotalCalculator, bulkDiscountCalculator, saleResponseFactory);
    }

    @Nested
    class WhenCreateSale {
        @BeforeEach
        void setUp() {
            when(saleRequest.getLineItems()).thenReturn(List.of(lineItem));
            when(productRepository.findByIds(any())).thenReturn(List.of(product));
            when(product.getId()).thenReturn(productId);
            when(product.getPrice()).thenReturn(price);
            when(lineSubtotalCalculator.calculate(any(), any())).thenReturn(lineSubtotal);
            when(saleResponseFactory.create(any(), any(), any(), any())).thenReturn(saleResponse);
            result = defaultSaleService.createSale(saleRequest);
        }

        @Test void shouldFindProductsByIds() {
            verify(productRepository).findByIds(List.of(productId));
        }

        @Test void shouldCalculateLineSubtotal() {
            verify(lineSubtotalCalculator).calculate(quantity, price);
        }

        @Test void shouldCreateSaleResponse() {
            verify(saleResponseFactory).create(saleRequest, List.of(product), List.of(lineSubtotal), lineSubtotal);
        }

        @Test void shouldReturnSaleResponse() {
            assertThat(result).isEqualTo(saleResponse);
        }
    }

    @Nested
    class WhenCreateSaleWithBulkDiscount {
        @BeforeEach
        void setUp() {
            when(saleRequest.getLineItems()).thenReturn(List.of(lineItem));
            when(productRepository.findByIds(any())).thenReturn(List.of(product));
            when(product.getId()).thenReturn(productId);
            when(product.getPrice()).thenReturn(price);
            when(lineSubtotalCalculator.calculate(any(), any())).thenReturn(lineSubtotal);
            when(bulkDiscountCalculator.calculate(any(), any())).thenReturn(lineDiscount);
            when(saleResponseFactory.createWithBulkDiscount(any(), any(), any(), any(), any(), any()))
                .thenReturn(saleResponseWithDiscount);
            resultWithDiscount = defaultSaleService.createSaleWithBulkDiscount(saleRequest);
        }

        @Test void shouldFindProductsByIds() {
            verify(productRepository).findByIds(List.of(productId));
        }

        @Test void shouldCalculateLineSubtotal() {
            verify(lineSubtotalCalculator).calculate(quantity, price);
        }

        @Test void shouldCalculateLineDiscount() {
            verify(bulkDiscountCalculator).calculate(quantity, lineSubtotal);
        }

        @Test void shouldCreateSaleResponseWithBulkDiscount() {
            verify(saleResponseFactory).createWithBulkDiscount(
                saleRequest,
                List.of(product),
                List.of(lineSubtotal),
                List.of(lineDiscount),
                lineDiscount,
                new BigDecimal("18.00"));
        }

        @Test void shouldReturnSaleResponseWithDiscount() {
            assertThat(resultWithDiscount).isEqualTo(saleResponseWithDiscount);
        }
    }
}
