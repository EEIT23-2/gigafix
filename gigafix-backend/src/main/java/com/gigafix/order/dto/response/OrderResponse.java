package com.gigafix.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.gigafix.order.entity.Order;

/**
 * 後端回傳完整訂單內容的回應 DTO。
 * 包含會員、金額、處理與付款狀態、收件資料、時間及 Order Service 組合的商品明細。
 */
public record OrderResponse(
		Long orderId,
		Long memberId,
		LocalDateTime orderDate,
		BigDecimal totalAmount,
		Order.OrderStatus status,
		Order.PaymentStatus paymentStatus,
		String receiverName,
		String receiverPhone,
		String shippingAddress,
		BigDecimal shippingFee,
		BigDecimal discountAmount,
		String remark,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<OrderItemResponse> items
) {
}
