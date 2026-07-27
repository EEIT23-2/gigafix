package com.gigafix.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
		Long orderItemId,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal subtotal,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
