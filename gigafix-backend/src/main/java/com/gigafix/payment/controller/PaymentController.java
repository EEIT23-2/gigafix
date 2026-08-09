package com.gigafix.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.payment.dto.request.CreatePaymentRequest;
import com.gigafix.payment.dto.response.PaymentResponse;
import com.gigafix.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members/{memberId}/orders/{orderId}/payment")
@RequiredArgsConstructor
/**
 * 提供會員建立與查詢自己訂單付款紀錄的 API。
 */
public class PaymentController {
	private final PaymentService paymentService;

	@PostMapping
	/** 建立指定會員訂單的唯一付款紀錄。 */
	public ResponseEntity<PaymentResponse> create(@PathVariable Long memberId, @PathVariable Long orderId,
			@Valid @RequestBody CreatePaymentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(memberId, orderId, request));
	}

	@GetMapping
	/** 查詢指定會員訂單的付款紀錄。 */
	public ResponseEntity<PaymentResponse> get(@PathVariable Long memberId, @PathVariable Long orderId) {
		return ResponseEntity.ok(paymentService.getPayment(memberId, orderId));
	}
}
