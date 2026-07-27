package com.gigafix.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.order.dto.request.CreateOrderRequest;
import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(
			@Valid @RequestBody CreateOrderRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(orderService.createOrder(request));
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(
			@PathVariable Long orderId
	) {
		return ResponseEntity.ok(orderService.getOrder(orderId));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<OrderResponse>> getOrdersByUser(
			@PathVariable Long userId
	) {
		return ResponseEntity.ok(orderService.getOrdersByUser(userId));
	}

	@PatchMapping("/{orderId}/status")
	public ResponseEntity<OrderResponse> updateOrderStatus(
			@PathVariable Long orderId,
			@Valid @RequestBody UpdateOrderStatusRequest request
	) {
		return ResponseEntity.ok(
				orderService.updateOrderStatus(orderId, request)
		);
	}

	@PatchMapping("/{orderId}/payment-status")
	public ResponseEntity<OrderResponse> updatePaymentStatus(
			@PathVariable Long orderId,
			@Valid @RequestBody UpdatePaymentStatusRequest request
	) {
		return ResponseEntity.ok(
				orderService.updatePaymentStatus(orderId, request)
		);
	}

	@DeleteMapping("/{orderId}")
	public ResponseEntity<Void> deleteOrder(
			@PathVariable Long orderId
	) {
		orderService.deleteOrder(orderId);
		return ResponseEntity.noContent().build();
	}
}
