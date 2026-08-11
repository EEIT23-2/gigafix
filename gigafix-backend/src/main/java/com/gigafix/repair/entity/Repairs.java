package com.gigafix.repair.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.gigafix.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity @Table(name = "repairs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repairs {
	
	@Id@Column(name = "repair_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "technician_id")
	private RepairTechnicians repairTechnicians; 
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "store_id", nullable = false)
	private Stores store;
	
	@Column(name = "repair_brand", nullable = false)
	private String repairBrand;
	
	@Column(name = "repair_model", nullable = false)
	private String repairModel;
	
	@Column(name = "issue_description", nullable = false)
	private String issueDescription;
	
	@Column(name = "booking_date", nullable = false)
	private LocalDate bookingDate;
	
	@Column(name = "time_slot", nullable = false)
	private LocalTime timeSlot;
	
	@Column(name = "dropoff_type", nullable = false)
	private Byte dropoffType;
	
	@Column(name = "repair_status", nullable = false)
	private Byte repairStatus;
	
	
	@Column(name = "serial_number")
	private String serialNumber;
	
	@Column(name = "inspection_result")
	private String inspectionResult;
	
	@Column(name = "repair_items")
	private String repairItems;
	
	@Column(name = "estimated_cost")
	private Integer estimatedCost;
	
	@Column(name = "approval_status")
	private Byte approvalStatus;
	
	@Column(name = "final_cost")
	private Integer finalCost;
	
	@Column(name = "repair_pay")
	private Byte repairPay;
	
	@Column(name = "repair_pay_status")
	private Byte repairPayStatus;
	
	@Column(name = "pickup_type")
	private Byte pickupType;
	
	@Column(name = "recipient_name")
	private String recipientName;
	
	@Column(name = "recipient_phone")
	private String recipientPhone;
	
	@Column(name = "recipient_address")
	private String recipientAddress;
	
	@Column(name = "repair_created_time", nullable = false)
	private LocalDateTime repairCreatedTime;
	
	@Column(name = "repair_updated_time", nullable = false)
	private LocalDateTime repairUpdatedTime;
	
	
	
}
