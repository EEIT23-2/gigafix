package com.gigafix.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
		@NotNull
		@Positive
		Long userId,

		@NotNull
		@Positive
		Long productId,

		@NotNull
		@Min(1)
		Integer quantity
) {
}
