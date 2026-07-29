package com.gigafix.order.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 建立訂單時描述單筆商品的請求 DTO。
 * 前端傳入商品識別碼、名稱、單價與數量，作為 CreateOrderRequest 的明細內容。
 */
public record CreateOrderItemRequest(
		@NotNull
		@Positive
		Long productId,

		@NotBlank
		@Size(max = 100)
		String productName,

		@NotNull
		@DecimalMin(value = "0.0", inclusive = true)
		BigDecimal unitPrice,

		@NotNull
		@Min(1)
		Integer quantity
) {
}
