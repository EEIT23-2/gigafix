package com.gigafix.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 前端加入購物車商品時傳入的請求 DTO。
 * {@code productId} 指定唯一二手機，Controller 驗證後交由 Cart Service 處理。
 */
public record AddCartItemRequest(
		@NotNull
		@Positive
		Long productId
) {
}
