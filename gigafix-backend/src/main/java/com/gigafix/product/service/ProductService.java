package com.gigafix.product.service;

import com.gigafix.product.entity.Product;

public interface ProductService {
    //以id查詢商品
    Product getProductById(Integer productId);
}
