package com.gigafix.repair.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.gigafix.repair.entity.status.ApprovalStatus;
import com.gigafix.repair.entity.status.DropoffType;
import com.gigafix.repair.entity.status.PickupType;
import com.gigafix.repair.entity.status.RepairPay;
import com.gigafix.repair.entity.status.RepairPayStatus;
import com.gigafix.repair.entity.status.RepairStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairsResponse{
	
//	1. 客戶填寫維修預約單
	private Long id;
	private Long memberId;
	private String memberName;
	private String repairBrand;
	private String repairModel;
	private String issueDescription;
	private LocalDate bookingDate;
	private LocalTime timeSlot;
	private RepairStatus repairStatus; 
	private String storeName;
	private DropoffType dropoffType;
	
//	2. 分配技師、檢測後報價
	private Integer technicianId;
	private String technicianName;
	private String serialNumber;
	private String inspectionResult;
	private String repairItems;
	private Integer estimatedCost;
	private ApprovalStatus approvalStatus;

//	3. 結案
	private Integer finalCost;
	private RepairPay repairPay;
	private RepairPayStatus repairPayStatus;
	private PickupType pickupType;
	private LocalDateTime repairCreatedTime;
	private LocalDateTime repairUpdatedTime;
	

}
