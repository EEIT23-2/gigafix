package com.gigafix.shipment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.shipment.entity.Shipment;

/** 存取物流紀錄；正式上線前仍需完成 Repository integration 與 SQL migration。 */
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
	boolean existsByOrderOrderId(Long orderId);
	/** 依訂單與會員共同查詢，避免跨會員存取。 */
	Optional<Shipment> findByOrderOrderIdAndOrderMemberId(Long orderId, Long memberId);
}
