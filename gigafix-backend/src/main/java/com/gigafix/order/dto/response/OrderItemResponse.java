package com.gigafix.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 後端回傳單筆訂單商品明細的回應 DTO。
 * 保存商品名稱、結帳單價、數量、小計及時間，避免 Controller 直接回傳 OrderItem Entity。
 */
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
