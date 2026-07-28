package com.gigafix.cart.controller;

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

import com.gigafix.cart.dto.request.AddCartItemRequest;
import com.gigafix.cart.dto.request.UpdateCartItemRequest;
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
	 * 將前端指定的商品與數量加入會員購物車。
	 *
	 * @param memberId 目前操作購物車的會員識別碼
	 * @param request 包含商品識別碼與加入數量的請求資料
	 * @return 新增或累加後的購物車項目，HTTP 狀態為 201 Created
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
	 * 修改會員購物車中指定項目的數量。
	 *
	 * @param memberId 目前操作購物車的會員識別碼
	 * @param cartItemId 要修改的購物車項目識別碼
	 * @param request 包含新數量的請求資料
	 * @return 更新後的購物車項目，HTTP 狀態為 200 OK
	 */
	@PatchMapping("/items/{cartItemId}")
	public ResponseEntity<CartItemResponse> updateQuantity(
			@PathVariable Long memberId,
			@PathVariable Long cartItemId,
			@Valid @RequestBody UpdateCartItemRequest request
	) {
		return ResponseEntity.ok(
				cartService.updateQuantity(memberId, cartItemId, request)
		);
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
