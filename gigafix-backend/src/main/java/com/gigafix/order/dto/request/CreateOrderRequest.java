package com.gigafix.order.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 前端建立訂單時傳入的請求 DTO。
 * 集中收件資料、運費、折扣、備註與商品明細，供 Controller 驗證後交由訂單流程使用。
 */
public record CreateOrderRequest(
		@NotBlank
		@Size(max = 50)
		String receiverName,

		@NotBlank
		@Size(max = 20)
		String receiverPhone,

		@NotBlank
		@Size(max = 255)
		String shippingAddress,

		@NotNull
		@DecimalMin(value = "0.0", inclusive = true)
		BigDecimal shippingFee,

		@NotNull
		@DecimalMin(value = "0.0", inclusive = true)
		BigDecimal discountAmount,

		@Size(max = 255)
		String remark,

		@NotEmpty
		List<@Valid CreateOrderItemRequest> items
) {
}
