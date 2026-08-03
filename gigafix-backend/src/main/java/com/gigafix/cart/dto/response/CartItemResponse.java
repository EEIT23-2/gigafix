package com.gigafix.cart.dto.response;

import java.time.LocalDateTime;

/**
 * 後端回傳單筆購物車項目的回應 DTO。
 * 包含項目、購物車、唯一商品及時間，避免 Controller 直接回傳 CartItem Entity。
 */
public record CartItemResponse(
		Long cartItemId,
		Long cartId,
		Long productId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
