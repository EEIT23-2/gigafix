package com.gigafix.cart.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.LocalDateTime;

//Response 前端顯示購物車明細所需要的資訊
@Getter
@Setter
@NoArgsConstructor //NoArgsConstructor 會自動生成一個無參構造函數
@AllArgsConstructor //AllArgsConstructor 會自動生成一個包含所有屬性的構造函數
@Builder
public class CartItemResponse {

    private Long cartItemId; // 購物車明細 ID
    private Long productId; // 商品 ID
    private String productName; // 商品名稱
    private String imageUrl; // 商品圖片
    private Integer price; // 商品價格
    private Integer saleStatus; // 商品銷售狀態
    private LocalDateTime createdAt; // 加入購物車時間
}