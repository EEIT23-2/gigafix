package com.gigafix.coupon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.coupon.dto.CouponResponse;
import com.gigafix.coupon.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gigafix/members/me/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // 取得目前可使用優惠券
    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons() {

        List<CouponResponse> responses = couponService.getAvailableCoupons();

        return ResponseEntity.ok(responses);
    }
}