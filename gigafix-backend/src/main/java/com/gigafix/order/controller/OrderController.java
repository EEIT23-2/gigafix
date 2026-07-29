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

/**
 * 訂單模組的 HTTP API 入口。
 * 接收會員的訂單查詢與狀態操作，交由 {@link OrderService} 執行商業邏輯後回傳訂單 DTO。
 */
@RestController
@RequestMapping("/api/members/{memberId}/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	/**
	 * 查詢會員的全部訂單，結果依建立時間由新到舊排列。
	 *
	 * @param memberId 要查詢訂單的會員識別碼
	 * @return 訂單摘要清單，HTTP 狀態為 200 OK
	 */
	@GetMapping
	public ResponseEntity<List<OrderResponse>> getOrdersByMember(
			@PathVariable Long memberId
	) {
		return ResponseEntity.ok(orderService.getOrdersByMember(memberId));
	}

	/**
	 * 查詢會員擁有的單筆訂單與訂單項目。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要查詢的訂單識別碼
	 * @return 完整訂單資料，HTTP 狀態為 200 OK
	 */
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(
			@PathVariable Long memberId,
			@PathVariable Long orderId
	) {
		return ResponseEntity.ok(orderService.getOrder(memberId, orderId));
	}

	/**
	 * 更新會員指定訂單的處理狀態。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要更新的訂單識別碼
	 * @param request 包含新訂單狀態的請求資料
	 * @return 更新後的訂單資料，HTTP 狀態為 200 OK
	 */
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

	/**
	 * 更新會員指定訂單的付款狀態。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要更新的訂單識別碼
	 * @param request 包含新付款狀態的請求資料
	 * @return 更新後的訂單資料，HTTP 狀態為 200 OK
	 */
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

	/**
	 * 刪除會員擁有的指定訂單及其訂單項目。
	 *
	 * @param memberId 訂單所屬會員的識別碼
	 * @param orderId 要刪除的訂單識別碼
	 * @return 無回應內容，HTTP 狀態為 204 No Content
	 */
	@DeleteMapping("/{orderId}")
	public ResponseEntity<Void> deleteOrder(
			@PathVariable Long memberId,
			@PathVariable Long orderId
	) {
		orderService.deleteOrder(memberId, orderId);
		return ResponseEntity.noContent().build();
	}
}
