package com.gigafix.order.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
		@NotNull
		@Positive
		Long userId,

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
		@Valid
		List<CreateOrderItemRequest> items
) {
}
