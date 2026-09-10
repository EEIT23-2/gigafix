package com.gigafix.order.ecpay.controller;

import java.util.Map;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.gigafix.common.util.SecurityUtils;
import com.gigafix.order.ecpay.service.EcpayPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EcpayPaymentController {

    private final EcpayPaymentService ecpayPaymentService;

    /**
     * 會員啟動 ECPay 信用卡付款。
     *
     * Backend 產生 HTML Form，
     * Browser 再自動 POST 到 ECPay Stage。
     */
    @GetMapping(value = "/api/gigafix/members/me/orders/{orderId}/ecpay-payment", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> startEcpayPayment(
            Authentication authentication,
            @PathVariable Long orderId) {

        Long memberId = SecurityUtils
                .getCurrentMember(authentication)
                .getId();

        String html = ecpayPaymentService.buildPaymentHtml(
                memberId,
                orderId);

        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @PostMapping(value = "/api/gigafix/ecpay/payment/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handlePaymentReturn(
            @RequestParam Map<String, String> callback) {

        try {

            ecpayPaymentService.processPaymentReturn(
                    callback);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("1|OK");

        } catch (IllegalArgumentException
                | IllegalStateException e) {

            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("0|Error");
        }
    }

    @PostMapping(value = "/api/gigafix/ecpay/payment/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handlePaymentResult(
            @RequestParam Map<String, String> callback) {

        String redirectUrl = ecpayPaymentService
                .processPaymentResult(callback);

        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirectUrl))
                .build();
    }
}