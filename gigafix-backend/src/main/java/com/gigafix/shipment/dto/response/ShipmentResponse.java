package com.gigafix.shipment.dto.response;

import java.time.LocalDateTime;

import com.gigafix.shipment.enums.ShippingMethod;
import com.gigafix.shipment.enums.ShippingStatus;
import com.gigafix.order.enums.OrderType;

/** 提供物流狀態與收件快照，不直接暴露關聯的訂單 Entity。 */
public record ShipmentResponse(
		Long shipmentId, Long orderId, OrderType orderType, String receiverName, String receiverPhone,
		String receiverAddress, ShippingMethod shippingMethod, String trackingNumber,
		ShippingStatus shippingStatus, LocalDateTime shippedAt, LocalDateTime deliveredAt,
		LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
