package com.gigafix.order.ecpay.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.order.ecpay.service.EcpayLogisticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gigafix/ecpay/logistics")
@RequiredArgsConstructor
public class EcpayLogisticsController {

    private final EcpayLogisticsService ecpayLogisticsService;

    @GetMapping(value = "/store-map", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> openStoreMap(
            @RequestParam String logisticsSubType) {

        String html = ecpayLogisticsService
                .buildStoreMapHtml(
                        logisticsSubType);

        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @PostMapping("/store-callback")
    public ResponseEntity<Void> handleStoreCallback(

            @RequestParam("MerchantID") String merchantId,

            @RequestParam("LogisticsSubType") String logisticsSubType,

            @RequestParam("CVSStoreID") String storeId,

            @RequestParam("CVSStoreName") String storeName,

            @RequestParam("CVSAddress") String storeAddress) {

        String redirectUrl = ecpayLogisticsService
                .buildCheckoutRedirectUrl(
                        merchantId,
                        logisticsSubType,
                        storeId,
                        storeName,
                        storeAddress);

        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirectUrl))
                .build();
    }
}