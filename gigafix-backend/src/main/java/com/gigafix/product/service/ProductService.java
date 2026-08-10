package com.gigafix.product.service;

import com.gigafix.product.dto.ProductQueryParams;
import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    //查詢商品列表
    Page<Product> getProducts(ProductQueryParams productQueryParams);
    //以id查詢商品
    Product getProductById(Long productId);
    //新增商品
    Long createProduct(ProductRequest productRequest);
    //修改商品 沒有回傳值
    void updateProduct(Long productId , ProductRequest productRequest);
    //刪除商品 沒有回傳值
    void deleteProductById(Long productId);

    //刪除所有商品
    void deleteAllProducts();

    //匯入商品表json檔

    int importProducts() throws IOException;

    //輸出商品表json檔

    byte[] exportProducts() throws IOException;

    //----以下商業邏輯for 訂單同學呼叫改狀態
    //鎖定商品  購買後結帳前呼叫
    void reserveProduct(Long productId);
    //釋放商品鎖定 取消訂單時呼叫用的
    void releaseProduct(Long productId);
    //確認售出 付款成功後呼叫
    void sellProduct(Long productId);

}
