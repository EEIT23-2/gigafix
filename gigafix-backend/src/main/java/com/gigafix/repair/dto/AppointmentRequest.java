package com.gigafix.repair.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.gigafix.repair.entity.status.DropoffType;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppointmentRequest {
// 客戶填寫預約單
	@NotNull(message = "會員 ID 不可為空")
	private Long memberId;
	
	@NotNull(message = "請選擇分店")
	private Byte storeId;
	
	@NotNull(message = "請填寫手機品牌")
	@Size(max = 20)
	private String repairBrand;
	
	@NotNull(message = "請填寫手機型號")
	@Size(max = 50)
	private String repairModel;
	
	@NotNull(message = "請描述故障狀況")
	@Size(max = 200)
	private String issueDescription;
	
	@NotNull(message = "請選擇預約日期")
	@FutureOrPresent(message = "預約日期不可為過去")
	private LocalDate bookingDate;
	
	@NotNull(message = "請選擇預約時段")
	private LocalTime timeSlot;
	
	@NotNull(message = "請選擇送修方式")
	private DropoffType dropoffType;
}
