package com.gigafix.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 前端加入購物車商品時傳入的請求 DTO。
 * {@code productId} 指定商品，{@code quantity} 指定加入數量，Controller 驗證後交由 Cart Service 處理。
 */
public record AddCartItemRequest(
		@NotNull
		@Positive
		Long productId,

		@NotNull
		@Min(1)
		Integer quantity
) {
}
