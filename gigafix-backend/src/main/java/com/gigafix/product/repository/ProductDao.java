package com.gigafix.product.repository;

import com.gigafix.product.constant.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.product.entity.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductDao extends JpaRepository<Product,Long> {


    //用JPQL實作條件查詢
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:search IS NULL OR p.productName LIKE %:search%) AND"+
            "(:modelName IS NULL OR p.productName LIKE %:modelName%) AND " +
            "(:color IS NULL OR p.productName LIKE %:color%) AND " +
            "(:storage IS NULL OR p.productName LIKE %:storage%)")
    List<Product> findByConditions(@Param("category") ProductCategory category,
                                   @Param("search") String search,
                                   @Param("modelName") String modelName,
                                   @Param("color") String color,
                                   @Param("storage") String storage);
}
