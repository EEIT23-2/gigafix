package com.gigafix.product.controller;

import com.gigafix.product.model.Product;
import com.gigafix.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class    ProductController {
    @Autowired
    private ProductService productService;
    //Id搜尋商品的路由controller
    @GetMapping("/products/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable Integer productId){
        Product product = productService.getProductById(productId);
        //回傳狀態 ,若找不到 回傳404並用.build()建body
        if(product !=null){
            return ResponseEntity.status(HttpStatus.OK).body(product);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

    }

}
