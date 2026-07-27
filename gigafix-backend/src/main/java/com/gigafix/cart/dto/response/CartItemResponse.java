package com.gigafix.cart.dto.response;

import java.time.LocalDateTime;

public record CartItemResponse(
		Long cartItemId,
		Long cartId,
		Long productId,
		Integer quantity,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
