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

@RestController
@RequestMapping("/api/members/{memberId}/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;
	private final OrderService orderService;

	@PostMapping("/items")
	public ResponseEntity<CartItemResponse> addCartItem(
			@PathVariable Long memberId,
			@Valid @RequestBody AddCartItemRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(cartService.addCartItem(memberId, request));
	}

	@GetMapping
	public ResponseEntity<CartResponse> getActiveCart(
			@PathVariable Long memberId
	) {
		return ResponseEntity.ok(cartService.getActiveCart(memberId));
	}

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

	@DeleteMapping("/items/{cartItemId}")
	public ResponseEntity<Void> deleteCartItem(
			@PathVariable Long memberId,
			@PathVariable Long cartItemId
	) {
		cartService.deleteCartItem(memberId, cartItemId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/checkout")
	public ResponseEntity<OrderResponse> checkoutCart(
			@PathVariable Long memberId
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(orderService.checkoutCart(memberId));
	}
}
