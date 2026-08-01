package com.gigafix.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.product.entity.Product;

public interface ProductDao extends JpaRepository<Product,Long> {
}
