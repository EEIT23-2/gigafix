package com.gigafix.coupon.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.coupon.entity.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCouponCodeIgnoreCase(String couponCode);

    List<Coupon> findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            LocalDateTime startAt,
            LocalDateTime endAt);
}