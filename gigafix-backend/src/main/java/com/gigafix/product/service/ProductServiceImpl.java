package com.gigafix.product.service;

import com.gigafix.product.model.Product;
import com.gigafix.product.repository.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceImpl implements ProductService   {
    @Autowired
    private ProductDao productDao;
    //實作以id查詢商品
    @Override
    public Product getProductById(Integer productId) {
        return productDao.findById(productId).orElse(null);
    }
}
