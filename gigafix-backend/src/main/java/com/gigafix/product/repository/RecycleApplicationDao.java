package com.gigafix.product.repository;

import com.gigafix.product.entity.Product;
import com.gigafix.product.entity.RecycleApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecycleApplicationDao extends JpaRepository<RecycleApplication,Long> {
}
