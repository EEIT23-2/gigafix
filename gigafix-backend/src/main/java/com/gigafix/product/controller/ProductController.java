package com.gigafix.product.controller;

import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.dto.ProductQueryParams;
import com.gigafix.product.dto.ProductRequest;
import com.gigafix.product.entity.Product;
import com.gigafix.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class    ProductController {
    @Autowired
    private ProductService productService;

    //查詢商品列表  (條件查詢:類別查詢,關鍵字查詢 ;Page<>介面做分頁&總頁數計算)
    @GetMapping("/api/gigafix/products")
    public ResponseEntity<Page<Product>> getProducts(ProductQueryParams productQueryParams){
        Page<Product> pageResult = productService.getProducts(productQueryParams);
        return ResponseEntity.status(HttpStatus.OK).body(pageResult);
    }


    //Id搜尋商品的路由controller
    @GetMapping("/api/gigafix/products/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable Long productId){
        Product product = productService.getProductById(productId);
        //回傳狀態 ,若找不到 回傳404並用.build()建body
        if(product !=null){
            return ResponseEntity.status(HttpStatus.OK).body(product);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 後台查詢商品列表：查詢邏輯與前台相同，但使用 /api/admin/** 路徑交由管理員 Security Filter Chain 驗證
    @GetMapping("/api/admin/products")
    public ResponseEntity<Page<Product>> getAdminProducts(ProductQueryParams productQueryParams){
        Page<Product> pageResult = productService.getProducts(productQueryParams);
        return ResponseEntity.status(HttpStatus.OK).body(pageResult);
    }

    // 後台依 ID 查詢商品：回傳內容與前台相同，僅路徑改由 Spring Security 保護
    @GetMapping("/api/admin/products/{productId}")
    public ResponseEntity<Product> getAdminProduct(@PathVariable Long productId){
        Product product = productService.getProductById(productId);
        if(product != null){
            return ResponseEntity.status(HttpStatus.OK).body(product);
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    //id新增商品的路由
    @PostMapping(value = "/api/admin/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createProduct(
            @RequestPart("product") @Valid ProductRequest productRequest,
            @RequestPart(value = "file", required = false) MultipartFile imageFile
    ) throws IOException {
        // JSON 商品資料與本機圖片分開接收，圖片網址由 Service 上傳 Cloudinary 後產生。
        Long productId = productService.createProduct(productRequest, imageFile);
        Product product = productService.getProductById(productId);

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    //修改商品
    @PutMapping(value = "/api/admin/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId,
                                                 @RequestPart("product") @Valid ProductRequest productRequest,
                                                 @RequestPart(value = "file", required = false) MultipartFile imageFile)
            throws IOException {
        Product product = productService.getProductById(productId);
        if (product ==null){   //先檢查是否有此id再做修改
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }//若找不到 回傳404給前端

        productService.updateProduct(productId, productRequest, imageFile);
        Product updatedProduct = productService.getProductById(productId);

        return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
    }
    //刪除商品
    @DeleteMapping("/api/admin/products/{productId}")
    public ResponseEntity<Product> deleteProduct(@PathVariable Long productId){
        productService.deleteProductById(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //刪除所有商品
    @DeleteMapping("/api/admin/products")
    public ResponseEntity<Void> deleteAllProducts(){//因不回傳任何Product物件 以Void泛型解偶
         productService.deleteAllProducts();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }


    //匯入json資料庫 路由
    @PostMapping("/api/admin/products/import")
    public ResponseEntity<Map<String, Object>> importProducts() throws IOException {

        int count = productService.importProducts();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "商品匯入成功");
        result.put("productCount", count);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    //將資料庫表格匯出為json檔
    @GetMapping("/api/admin/products/export")
    public ResponseEntity<byte[]> exportProducts() throws IOException{
        byte[] jsonBytes = productService.exportProducts();//取得json檔byte資料
        //以HttpHeader指定檔名&檔案類型
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=products.json");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(jsonBytes);
        //預設下載到Downloads資料夾
    }


    //----以下 api for 訂單管理者呼叫作做 結帳狀態防呆檢查機制-----

    //鎖定並保留商品 (結帳但未付款時,訂單人員可呼叫)
    @PutMapping("/api/admin/products/{productId}/reserve")
    public ResponseEntity<Product> reserveProduct(@PathVariable Long productId) {
        // 先檢查是否有此商品id
        Product product = productService.getProductById(productId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 找不到回傳 404
        }

        try {
            //  執行鎖定的邏輯（內有狀態檢查）
            productService.reserveProduct(productId);
            //  並回傳給前端http狀態與body內容
            Product updatedProduct = productService.getProductById(productId);
            return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    //  釋放商品鎖定（當取消訂單等等時,訂單人員可以呼叫）
    @PutMapping("/api/admin/products/{productId}/release")
    public ResponseEntity<Product> releaseProduct(@PathVariable Long productId) {
        // 先檢查是否有此商品id
        Product product = productService.getProductById(productId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 找不到回傳 404
        }
        // 執行釋放邏輯
        productService.releaseProduct(productId);
        // 回傳Http狀態&body訊息
        Product updatedProduct = productService.getProductById(productId);
        return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
    }

    // 確認售出（當付款成功時,由訂單人員叫）
    @PutMapping("/api/admin/products/{productId}/sell")
    public ResponseEntity<Product> sellProduct(@PathVariable Long productId) {
        // 先檢查是否有此商品id
        Product product = productService.getProductById(productId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 找不到回傳 404
        }

        try {
            // 執行確認售出邏輯
            productService.sellProduct(productId);
            // 回傳http狀態&body訊息給前端
            Product updatedProduct = productService.getProductById(productId);
            return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
        } catch (IllegalStateException e) {
            // 如果商品早已售出,拒絕重複購買,回傳 400
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
