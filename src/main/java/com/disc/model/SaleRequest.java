package com.disc.model;

import java.util.List;

public class SaleRequest {
    private List<SaleLineItemRequest> lineItems;

    public SaleRequest() {
    }

    public SaleRequest(List<SaleLineItemRequest> lineItems) {
        this.lineItems = lineItems;
    }

    public List<SaleLineItemRequest> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<SaleLineItemRequest> lineItems) {
        this.lineItems = lineItems;
    }
}
