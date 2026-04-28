package com.disc.factory;

import com.disc.entity.Product;
import com.disc.model.SaleRequest;
import com.disc.model.SaleResponse;
import com.disc.model.SaleResponseWithDiscount;

import java.math.BigDecimal;
import java.util.List;

public interface SaleResponseFactory {
    SaleResponse create(SaleRequest saleRequest,
                        List<Product> products,
                        List<BigDecimal> lineSubtotals,
                        BigDecimal saleTotal);

    SaleResponseWithDiscount createWithBulkDiscount(SaleRequest saleRequest,
                                                    List<Product> products,
                                                    List<BigDecimal> lineSubtotals,
                                                    List<BigDecimal> lineDiscounts,
                                                    BigDecimal totalDiscount,
                                                    BigDecimal saleTotal);
}
