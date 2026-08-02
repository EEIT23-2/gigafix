package com.gigafix.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 後端回傳單筆訂單商品明細的回應 DTO。
 * 保存唯一商品的名稱、成交單價及時間，避免 Controller 直接回傳 OrderItem Entity。
 */
public record OrderItemResponse(
		Long orderItemId,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
