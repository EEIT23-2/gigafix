package com.gigafix.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.service.OrderService;

import lombok.RequiredArgsConstructor;

/** 會員訂單唯讀查詢與取消 API。 */
@RestController
@RequestMapping("/api/members/{memberId}/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	/** 列出指定會員擁有的全部訂單。 */
	@GetMapping
	public ResponseEntity<List<OrderResponse>> getOrdersByMember(
			@PathVariable Long memberId
	) {
		return ResponseEntity.ok(orderService.getOrdersByMember(memberId));
	}

	/** 查詢指定會員擁有的單筆訂單。 */
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(
			@PathVariable Long memberId,
			@PathVariable Long orderId
	) {
		return ResponseEntity.ok(orderService.getOrder(memberId, orderId));
	}

	/** 取消指定會員自己的待處理訂單並回傳更新結果。 */
	@PatchMapping("/{orderId}/cancel")
	public ResponseEntity<OrderResponse> cancelOrder(
			@PathVariable Long memberId,
			@PathVariable Long orderId
	) {
		return ResponseEntity.ok(orderService.cancelOrder(memberId, orderId));
	}
}
