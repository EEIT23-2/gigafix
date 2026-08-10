package com.gigafix.product.repository;

import com.gigafix.product.constant.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            "(:storage IS NULL OR p.productName LIKE %:storage%) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " + // 💡 新增最低價判斷
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findByConditions(@Param("category") ProductCategory category,
                                   @Param("search") String search,
                                   @Param("modelName") String modelName,
                                   @Param("color") String color,
                                   @Param("storage") String storage,
                                   @Param("minPrice") Integer minPrice,
                                   @Param("maxPrice") Integer maxPrice,
                                   Pageable page); //新增分頁功能
}

