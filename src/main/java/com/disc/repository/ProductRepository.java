package com.disc.repository;

import com.disc.entity.Product;

import java.util.List;
import java.util.UUID;

public interface ProductRepository {
    List<Product> findByIds(List<UUID> productIds);
}
