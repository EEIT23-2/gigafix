package com.gigafix.product.service;

import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

@Component
public class ProductServiceImpl implements ProductService   {
    @Autowired
    private ProductDao productDao;
    //實作以id查詢商品
    @Override
    public Product getProductById(Long productId) {
        return productDao.findById(productId).orElse(null);
    }

    //實作新增商品
    @Override
    public Long createProduct(ProductRequest productRequest) {
        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setCategory(productRequest.getCategory());
        product.setImageUrl(productRequest.getImageUrl());
        product.setDescription(productRequest.getDescription());
        product.setAppearance(productRequest.getAppearance());
        product.setGrade(productRequest.getGrade());
        product.setPrice(productRequest.getPrice());
        product.setSaleStatus(productRequest.getSaleStatus());


        product.setCreatedDate(LocalDateTime.now());
        product.setLastModifiedDate(LocalDateTime.now());

        Product savedProduct  = productDao.save(product);
        return savedProduct.getProductId();


    }
}

