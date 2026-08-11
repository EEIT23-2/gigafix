package com.gigafix.repair.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;



@Entity @Table(name = "repairs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repairs {
	
	@Id@Column(name = "repair_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	@ToString.Exclude
    @EqualsAndHashCode.Exclude
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "technician_id")
	@ToString.Exclude
    @EqualsAndHashCode.Exclude
	private RepairTechnicians repairTechnicians; 
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	@ToString.Exclude
    @EqualsAndHashCode.Exclude
	private Stores store;
	
	@Column(name = "repair_brand", nullable = false, length = 20)
	private String repairBrand;
	
	@Column(name = "repair_model", nullable = false, length = 50)
	private String repairModel;
	
	@Column(name = "issue_description", nullable = false, length = 200)
	private String issueDescription;
	
	@Column(name = "booking_date", nullable = false)
	private LocalDate bookingDate;
	
	@Column(name = "time_slot", nullable = false)
	private LocalTime timeSlot;
	
	@Column(name = "dropoff_type", nullable = false)
	private Byte dropoffType;
	
	@Column(name = "repair_status", nullable = false, columnDefinition = "tinyint default 0")
	private Byte repairStatus = 0;
	
	@Column(name = "serial_number", columnDefinition = "varchar(50)")
	private String serialNumber;
	
	@Column(name = "inspection_result", length = 500)
	private String inspectionResult;
	
	@Column(name = "repair_items", length = 500)
	private String repairItems;
	
	@Column(name = "estimated_cost", columnDefinition = "int default 0")
	private Integer estimatedCost = 0;
	
	@Column(name = "approval_status")
	private Byte approvalStatus;
	
	@Column(name = "final_cost", columnDefinition = "int default 0")
	private Integer finalCost = 0;
	
	@Column(name = "repair_pay")
	private Byte repairPay;
	
	@Column(name = "repair_pay_status")
	private Byte repairPayStatus;
	
	@Column(name = "pickup_type")
	private Byte pickupType;
	
	@Column(name = "recipient_name", length = 50)
	private String recipientName;
	
	@Column(name = "recipient_phone", columnDefinition = "varchar(20)")
	private String recipientPhone;
	
	@Column(name = "recipient_address", length = 200)
	private String recipientAddress;
	
	@CreationTimestamp
	@Column(name = "repair_created_time", nullable = false, updatable = false)
	private LocalDateTime repairCreatedTime;
	
	@UpdateTimestamp
	@Column(name = "repair_updated_time", nullable = false)
	private LocalDateTime repairUpdatedTime;
	
	
	
}
