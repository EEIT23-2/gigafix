package com.gigafix.repair.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.common.util.SecurityUtils;
import com.gigafix.repair.service.RepairEcpayPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RepairEcpayPaymentController {

	private final RepairEcpayPaymentService repairEcpayPaymentService;

	/**
	 * 客戶啟動綠界信用卡付款。
	 * Backend 產生 HTML Form，Browser 再自動 POST 到 ECPay Stage。
	 */
	@GetMapping(value = "/api/repairs/{id}/ecpay-payment", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> startEcpayPayment(@PathVariable Long id, Authentication authentication) {
		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		String html = repairEcpayPaymentService.buildPaymentHtml(memberId, id);

		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
	}

	@PostMapping(value = "/api/gigafix/ecpay/repair-payment/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> handlePaymentReturn(@RequestParam Map<String, String> callback) {
		try {
			repairEcpayPaymentService.processPaymentReturn(callback);
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("1|OK");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("0|Error");
		}
	}

	@PostMapping(value = "/api/gigafix/ecpay/repair-payment/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Void> handlePaymentResult(@RequestParam Map<String, String> callback) {
		String redirectUrl = repairEcpayPaymentService.processPaymentResult(callback);

		return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(redirectUrl)).build();
	}
}
