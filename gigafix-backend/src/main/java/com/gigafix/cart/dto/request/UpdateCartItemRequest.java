package com.gigafix.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 前端修改購物車項目時傳入的請求 DTO。
 * {@code quantity} 是要取代原數量的新值，Controller 驗證後交由 Cart Service 更新。
 */
public record UpdateCartItemRequest(
		@NotNull
		@Min(1)
		Integer quantity
) {
}
