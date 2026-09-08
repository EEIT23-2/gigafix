package com.gigafix.coupon.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.coupon.dto.CouponResponse;
import com.gigafix.coupon.entity.Coupon;
import com.gigafix.coupon.repository.CouponRepository;
import com.gigafix.coupon.entity.Coupon;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    // 取得目前可以使用的優惠券
    public List<CouponResponse> getAvailableCoupons() {

        LocalDateTime now = LocalDateTime.now();

        List<Coupon> coupons = couponRepository
                .findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        now,
                        now);

        List<CouponResponse> responses = new ArrayList<>();

        for (Coupon coupon : coupons) {

            CouponResponse response = CouponResponse.builder()
                    .couponCode(coupon.getCouponCode())
                    .couponName(coupon.getCouponName())
                    .discountAmount(coupon.getDiscountAmount())
                    .build();

            responses.add(response);
        }

        return responses;
    }
    // 驗證優惠券是否有效，若無效則拋出異常
    public Coupon validateCoupon(String couponCode) {

        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }

        Coupon coupon = couponRepository
                .findByCouponCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("優惠券不存在"));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new IllegalArgumentException("優惠券目前未啟用");
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(coupon.getStartAt())) {
            throw new IllegalArgumentException("優惠券尚未開始");
        }

        if (now.isAfter(coupon.getEndAt())) {
            throw new IllegalArgumentException("優惠券已過期");
        }

        return coupon;
    }
}