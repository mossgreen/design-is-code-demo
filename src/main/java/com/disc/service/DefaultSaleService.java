package com.disc.service;

import com.disc.entity.Product;
import com.disc.factory.SaleResponseFactory;
import com.disc.model.SaleLineItemRequest;
import com.disc.model.SaleRequest;
import com.disc.model.SaleResponse;
import com.disc.model.SaleResponseWithDiscount;
import com.disc.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DefaultSaleService implements SaleService {

    private final ProductRepository productRepository;
    private final LineSubtotalCalculator lineSubtotalCalculator;
    private final BulkDiscountCalculator bulkDiscountCalculator;
    private final SaleResponseFactory saleResponseFactory;

    public DefaultSaleService(ProductRepository productRepository,
                              LineSubtotalCalculator lineSubtotalCalculator,
                              BulkDiscountCalculator bulkDiscountCalculator,
                              SaleResponseFactory saleResponseFactory) {
        this.productRepository = productRepository;
        this.lineSubtotalCalculator = lineSubtotalCalculator;
        this.bulkDiscountCalculator = bulkDiscountCalculator;
        this.saleResponseFactory = saleResponseFactory;
    }

    @Override
    public SaleResponse createSale(SaleRequest saleRequest) {
        List<UUID> productIds = saleRequest.getLineItems().stream()
            .map(SaleLineItemRequest::getProductId)
            .toList();
        List<Product> products = productRepository.findByIds(productIds);
        Map<UUID, Product> productById = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        List<BigDecimal> lineSubtotals = new ArrayList<>();
        for (SaleLineItemRequest lineItem : saleRequest.getLineItems()) {
            Product product = productById.get(lineItem.getProductId());
            BigDecimal lineSubtotal = lineSubtotalCalculator.calculate(lineItem.getQuantity(), product.getPrice());
            lineSubtotals.add(lineSubtotal);
        }
        BigDecimal saleTotal = lineSubtotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return saleResponseFactory.create(saleRequest, products, lineSubtotals, saleTotal);
    }

    @Override
    public SaleResponseWithDiscount createSaleWithBulkDiscount(SaleRequest saleRequest) {
        List<UUID> productIds = saleRequest.getLineItems().stream()
            .map(SaleLineItemRequest::getProductId)
            .toList();
        List<Product> products = productRepository.findByIds(productIds);
        Map<UUID, Product> productById = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p));

        List<BigDecimal> lineSubtotals = new ArrayList<>();
        List<BigDecimal> lineDiscounts = new ArrayList<>();
        for (SaleLineItemRequest lineItem : saleRequest.getLineItems()) {
            Product product = productById.get(lineItem.getProductId());
            BigDecimal lineSubtotal = lineSubtotalCalculator.calculate(lineItem.getQuantity(), product.getPrice());
            lineSubtotals.add(lineSubtotal);
            BigDecimal lineDiscount = bulkDiscountCalculator.calculate(lineItem.getQuantity(), lineSubtotal);
            lineDiscounts.add(lineDiscount);
        }
        BigDecimal saleSubtotal = lineSubtotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = lineDiscounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saleTotal = saleSubtotal.subtract(totalDiscount);
        return saleResponseFactory.createWithBulkDiscount(saleRequest, products, lineSubtotals, lineDiscounts, totalDiscount, saleTotal);
    }
}
