package com.gigafix.coupon.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.gigafix.coupon.entity.Coupon;
import com.gigafix.coupon.repository.CouponRepository;

import lombok.RequiredArgsConstructor;
// 打開spring boot時自動執行
// 找不到WELCOME500這個資料時 會自動建立一張新的優惠券
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.coupon.enabled", havingValue = "true", matchIfMissing = true)
public class CouponDataInitializer
        implements CommandLineRunner {

    private static final String DEMO_COUPON_CODE = "WELCOME500";

    private final CouponRepository couponRepository;

    @Override
    public void run(String... args) {

        boolean couponExists = couponRepository
                .findByCouponCodeIgnoreCase(
                        DEMO_COUPON_CODE)
                .isPresent();

        if (couponExists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = new Coupon();

        coupon.setCouponCode(
                DEMO_COUPON_CODE);

        coupon.setCouponName(
                "新開幕優惠券");

        coupon.setDiscountAmount(500);

        coupon.setStartAt(
                now.minusDays(1));

        coupon.setEndAt(
                now.plusYears(5));

        coupon.setActive(true);

        couponRepository.save(coupon);
    }
}