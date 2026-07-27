package com.gigafix.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
		@NotNull
		@Min(1)
		Integer quantity
) {
}
