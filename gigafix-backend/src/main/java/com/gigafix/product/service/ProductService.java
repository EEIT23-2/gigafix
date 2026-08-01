package com.gigafix.product.service;

import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;

public interface ProductService {
    //以id查詢商品
    Product getProductById(Long productId);
    //新增商品
    Long createProduct(ProductRequest productRequest);
}
