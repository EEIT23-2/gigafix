package com.gigafix.repair.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.repair.entity.Repairs;

public interface RepairsRepository extends JpaRepository<Repairs, Long> {
	
	// 檢查同一分店、同一天、同一時段是否已經有維修單
   //  時段衝突邏輯不是「同一技師」而是「同一分店」
	Optional<Repairs> findByStore_IdAndBookingDateAndTimeSlot(Byte storeId, LocalDate bookingDate,
			LocalTime timeSlot);

}


    //修改：原本寫 findByStore，這樣寫不出來
	// store 欄位是關聯物件（Stores），不是 Byte，Spring Data 會照方法名稱去對應 Entity 的欄位，
	// findByStore 只會被解析成「比對 store 這個關聯物件本身」，需要一個 Stores 型別的參數，
	// 跟你傳進來的 3 個參數（Byte, LocalDate, LocalTime）對不起來，啟動應用程式時就會噴錯。
	// 要指定成「比對 store 底下的 id 欄位」，寫法是 findByStore_IdAnd...
