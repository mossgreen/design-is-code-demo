package com.disc.service;

import com.disc.model.SaleRequest;
import com.disc.model.SaleResponse;
import com.disc.model.SaleResponseWithDiscount;

public interface SaleService {
    SaleResponse createSale(SaleRequest saleRequest);

    SaleResponseWithDiscount createSaleWithBulkDiscount(SaleRequest saleRequest);
}
