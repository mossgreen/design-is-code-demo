package com.disc.model;

import java.util.UUID;

public class SaleLineItemRequest {
    private final UUID productId;
    private final Integer quantity;

    public SaleLineItemRequest(UUID productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
