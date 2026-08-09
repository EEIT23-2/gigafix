package com.gigafix.product.repository;

import com.gigafix.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecycleApplicationDao extends JpaRepository<Product,Long> {
}
