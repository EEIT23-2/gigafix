package com.gigafix.cart.dto.response;

import java.time.LocalDateTime;

public record CartItemResponse(
		Long cartItemId,
		Long userId,
		Long productId,
		Integer quantity,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
