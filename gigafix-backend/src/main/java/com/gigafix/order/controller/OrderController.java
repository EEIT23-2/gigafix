package com.gigafix.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.order.dto.request.UpdateOrderStatusRequest;
import com.gigafix.order.dto.request.UpdatePaymentStatusRequest;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members/{memberId}/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping
	public ResponseEntity<List<OrderResponse>> getOrdersByMember(
			@PathVariable Long memberId
	) {
		return ResponseEntity.ok(orderService.getOrdersByMember(memberId));
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(
			@PathVariable Long memberId,
			@PathVariable Long orderId
	) {
		return ResponseEntity.ok(orderService.getOrder(memberId, orderId));
	}

	@PatchMapping("/{orderId}/status")
	public ResponseEntity<OrderResponse> updateOrderStatus(
			@PathVariable Long memberId,
			@PathVariable Long orderId,
			@Valid @RequestBody UpdateOrderStatusRequest request
	) {
		return ResponseEntity.ok(
				orderService.updateOrderStatus(memberId, orderId, request)
		);
	}

	@PatchMapping("/{orderId}/payment-status")
	public ResponseEntity<OrderResponse> updatePaymentStatus(
			@PathVariable Long memberId,
			@PathVariable Long orderId,
			@Valid @RequestBody UpdatePaymentStatusRequest request
	) {
		return ResponseEntity.ok(
				orderService.updatePaymentStatus(
						memberId,
						orderId,
						request
				)
		);
	}

	@DeleteMapping("/{orderId}")
	public ResponseEntity<Void> deleteOrder(
			@PathVariable Long memberId,
			@PathVariable Long orderId
	) {
		orderService.deleteOrder(memberId, orderId);
		return ResponseEntity.noContent().build();
	}
}
