package com.gigafix.order.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gigafix.order.dto.request.CreateOrderItemRequest;
import com.gigafix.order.dto.request.CreateOrderRequest;

import jakarta.validation.Validation;

class CreateOrderRequestValidationTest {

	@Test
	void itemsValidateContainerElements() {
		try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
			var request = new CreateOrderRequest(
					"收件人",
					"0900000000",
					"測試地址",
					BigDecimal.ZERO,
					BigDecimal.ZERO,
					null,
					List.of(new CreateOrderItemRequest(
							null,
							"商品",
							BigDecimal.TEN,
							1
					))
			);

			var violations = validatorFactory.getValidator().validate(request);

			assertTrue(violations.stream().anyMatch(violation ->
					violation.getPropertyPath().toString()
							.contains("items[0].productId")
			));
		}
	}

	@Test
	void itemsValidateNestedQuantity() {
		try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
			var request = new CreateOrderRequest(
					"收件人",
					"0900000000",
					"測試地址",
					BigDecimal.ZERO,
					BigDecimal.ZERO,
					null,
					List.of(new CreateOrderItemRequest(
							10L,
							"商品",
							BigDecimal.TEN,
							0
					))
			);

			var violations = validatorFactory.getValidator().validate(request);

			assertTrue(violations.stream().anyMatch(violation ->
					violation.getPropertyPath().toString()
							.contains("items[0].quantity")
			));
		}
	}
}
