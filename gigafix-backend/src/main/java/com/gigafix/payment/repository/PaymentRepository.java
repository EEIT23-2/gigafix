package com.gigafix.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.payment.entity.Payment;

/** 存取付款紀錄；正式上線前仍需完成 Repository integration 與 SQL migration。 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	/** 依付款與會員共同查詢，避免跨會員存取。 */
	Optional<Payment> findByPaymentIdAndOrderMemberId(Long paymentId, Long memberId);
	/** 依訂單與會員共同查詢，避免洩漏其他會員的付款紀錄。 */
	Optional<Payment> findByOrderOrderIdAndOrderMemberId(Long orderId, Long memberId);
	boolean existsByOrderOrderId(Long orderId);
}
