package com.gigafix.shipment.mapper;

import org.springframework.stereotype.Component;

import com.gigafix.shipment.dto.response.ShipmentResponse;
import com.gigafix.shipment.entity.Shipment;

@Component
/** 將 Shipment Entity 轉為回應 DTO，不負責查詢或業務判斷。 */
public class ShipmentMapper {
	/** 輸出收件快照與訂單識別碼，不直接回傳關聯 Entity。 */
	public ShipmentResponse toResponse(Shipment shipment) {
		return new ShipmentResponse(shipment.getShipmentId(), shipment.getOrder().getOrderId(), shipment.getOrder().getOrderType(),
				shipment.getReceiverName(), shipment.getReceiverPhone(), shipment.getReceiverAddress(),
				shipment.getShippingMethod(), shipment.getTrackingNumber(), shipment.getShippingStatus(),
				shipment.getShippedAt(), shipment.getDeliveredAt(), shipment.getCreatedAt(), shipment.getUpdatedAt());
	}
}
