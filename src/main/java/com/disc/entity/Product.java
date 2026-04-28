package com.disc.entity;

import java.math.BigDecimal;
import java.util.UUID;

public interface Product {
    UUID getId();
    BigDecimal getPrice();
}
