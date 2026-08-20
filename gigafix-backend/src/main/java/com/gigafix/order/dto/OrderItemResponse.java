package com.gigafix.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long productId;      // 商品 ID

    private String productName;  // 下單時商品名稱

    private Integer unitPrice;   // 下單時商品成交價格
}