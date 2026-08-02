package com.gigafix.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.gigafix.order.entity.Order;
import com.gigafix.order.enums.OrderType;

/**
 * 後端回傳完整訂單內容的回應 DTO。
 * 包含會員、訂單類型、金額、狀態、時間及 Order Service 組合的商品快照。
 */
public record OrderResponse(
		Long orderId,
		Long memberId,
		OrderType orderType,
		LocalDateTime orderDate,
		BigDecimal totalAmount,
		Order.OrderStatus status,
		Order.PaymentStatus paymentStatus,
		String remark,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<OrderItemResponse> items
) {
}
