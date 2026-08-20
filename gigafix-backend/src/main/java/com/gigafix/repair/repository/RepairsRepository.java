package com.gigafix.repair.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.repair.entity.Repairs;

public interface RepairsRepository extends JpaRepository<Repairs, Long> {
	
	// 檢查同一分店、同一天、同一時段是否已經有維修單
   //  時段衝突邏輯不是「同一技師」而是「同一分店」
	Optional<Repairs> findByStore(Byte storeId, LocalDate bookingDate,
			LocalTime timeSlot);

}
