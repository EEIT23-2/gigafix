package com.gigafix.cart.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.gigafix.cart.entity.Cart;

/**
 * 後端回傳會員購物車完整內容的回應 DTO。
 * 包含購物車識別碼、會員、狀態、時間及 Cart Service 組合的商品項目清單。
 */
public record CartResponse(
		Long cartId,
		Long memberId,
		Cart.CartStatus status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<CartItemResponse> items
) {
}
