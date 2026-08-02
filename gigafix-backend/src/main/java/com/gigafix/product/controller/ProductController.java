package com.gigafix.product.controller;

import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.dto.ProductQueryParams;
import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class    ProductController {
    @Autowired
    private ProductService productService;

    //查詢商品列表  (條件查詢:類別查詢,關鍵字查詢)
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts(ProductQueryParams productQueryParams){
        List<Product> productList = productService.getProducts(productQueryParams);
        return ResponseEntity.status(HttpStatus.OK).body(productList);
    }


    //Id搜尋商品的路由controller
    @GetMapping("/products/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable Long productId){
        Product product = productService.getProductById(productId);
        //回傳狀態 ,若找不到 回傳404並用.build()建body
        if(product !=null){
            return ResponseEntity.status(HttpStatus.OK).body(product);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    //id新增商品的路由
    @PostMapping("/products")      //@Valid 是為了讓@NotNull生效
    public ResponseEntity<Product> createProduct(@RequestBody @Valid ProductRequest productRequest){
        Long productId  = productService.createProduct(productRequest);
        Product product = productService.getProductById(productId);

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    //修改商品
    @PutMapping("/products/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId,
                                                 @RequestBody @Valid ProductRequest productRequest){
        Product product = productService.getProductById(productId);
        if (product ==null){   //先檢查是否有此id再做修改
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }//若找不到 回傳404給前端

        productService.updateProduct(productId,productRequest);
        Product updatedProduct = productService.getProductById(productId);

        return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
    }
    //刪除商品
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Product> deleteProduct(@PathVariable Long productId){
        productService.deleteProductById(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}