package com.gigafix.product.service;

import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.repository.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Transactional
@Service
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
    //實作修改商品
    @Override
    public void updateProduct(Long productId, ProductRequest productRequest) {
        Optional<Product> product = productDao.findById(productId);
        //檢查是否有該商品 後再做修改
        if(product.isPresent()){
            Product gotProduct = product.get();
            gotProduct.setProductName(productRequest.getProductName());
            gotProduct.setCategory(productRequest.getCategory());
            gotProduct.setImageUrl(productRequest.getImageUrl());
            gotProduct.setDescription(productRequest.getDescription());
            gotProduct.setAppearance(productRequest.getAppearance());
            gotProduct.setGrade(productRequest.getGrade());
            gotProduct.setPrice(productRequest.getPrice());
            gotProduct.setSaleStatus(productRequest.getSaleStatus());

            gotProduct.setLastModifiedDate(LocalDateTime.now());

            Product updatedProduct = productDao.save(gotProduct);
        }else{
            return; //若有商品直接返回
        }

    }
}

