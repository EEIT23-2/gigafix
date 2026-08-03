package com.gigafix.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.response.CartItemResponse;
import com.gigafix.cart.dto.response.CartResponse;
import com.gigafix.cart.service.CartService;
import com.gigafix.order.dto.response.OrderResponse;
import com.gigafix.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 購物車模組的 HTTP API 入口。
 * 接收會員購物車相關請求，將輸入交由 {@link CartService} 處理，並以購物車或訂單回應 DTO 回傳結果。
 */
@RestController
@RequestMapping("/api/members/{memberId}/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;
	private final OrderService orderService;

	/**
	 * 將前端指定的唯一商品加入會員購物車。
	 *
	 * @param memberId 目前操作購物車的會員識別碼
	 * @param request 包含商品識別碼的請求資料
	 * @return 新增後的購物車項目；商品已存在時拒絕，成功時 HTTP 狀態為 201 Created
	 */
	@PostMapping("/items")
	public ResponseEntity<CartItemResponse> addCartItem(
			@PathVariable Long memberId,
			@Valid @RequestBody AddCartItemRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(cartService.addCartItem(memberId, request));
	}

	/**
	 * 查詢會員目前啟用中的購物車及其所有項目。
	 *
	 * @param memberId 要查詢購物車的會員識別碼
	 * @return 購物車明細，HTTP 狀態為 200 OK
	 */
	@GetMapping
	public ResponseEntity<CartResponse> getActiveCart(
			@PathVariable Long memberId
	) {
		return ResponseEntity.ok(cartService.getActiveCart(memberId));
	}

	/**
	 * 刪除會員購物車中的指定項目。
	 *
	 * @param memberId 目前操作購物車的會員識別碼
	 * @param cartItemId 要刪除的購物車項目識別碼
	 * @return 無回應內容，HTTP 狀態為 204 No Content
	 */
	@DeleteMapping("/items/{cartItemId}")
	public ResponseEntity<Void> deleteCartItem(
			@PathVariable Long memberId,
			@PathVariable Long cartItemId
	) {
		cartService.deleteCartItem(memberId, cartItemId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 清空會員目前 ACTIVE 購物車中的所有項目。
	 *
	 * @param memberId 目前操作購物車的會員識別碼
	 * @return 無回應內容，HTTP 狀態為 204 No Content
	 */
	@DeleteMapping("/items")
	public ResponseEntity<Void> clearActiveCart(
			@PathVariable Long memberId
	) {
		cartService.clearActiveCart(memberId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 將會員目前的購物車交由訂單服務執行結帳。
	 * 商品資料尚未完成時，例外處理器會回傳 501 Not Implemented。
	 *
	 * @param memberId 要進行結帳的會員識別碼
	 * @return 結帳完成時回傳訂單資料，HTTP 狀態為 201 Created
	 */
	@PostMapping("/checkout")
	public ResponseEntity<OrderResponse> checkoutCart(
			@PathVariable Long memberId
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(orderService.checkoutCart(memberId));
	}
}
