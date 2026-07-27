package com.gigafix.cart.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.gigafix.cart.entity.Cart;

public record CartResponse(
		Long cartId,
		Long userId,
		Cart.CartStatus status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<CartItemResponse> items
) {
}
