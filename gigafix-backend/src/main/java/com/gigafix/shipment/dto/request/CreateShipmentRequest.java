package com.gigafix.shipment.dto.request;

import com.gigafix.shipment.enums.ShippingMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 建立物流紀錄時保存的收件資料與配送方式。 */
public record CreateShipmentRequest(
		@NotBlank @Size(max=50) String receiverName,
		@NotBlank @Size(max=20) String receiverPhone,
		@NotBlank @Size(max=255) String receiverAddress,
		@NotNull ShippingMethod shippingMethod
) {
}
