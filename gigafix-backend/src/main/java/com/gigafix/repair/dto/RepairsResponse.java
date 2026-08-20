package com.gigafix.repair.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairsResponse<Localtime> {
	
	private Long id;
	private String repairBrand;
	private String repairModel;
	private String issueDescription;
	private LocalDate bookingDate;
	private LocalTime timeslot;
	private Byte repairStatus; //（已收件/檢測中）
	private String StoreName;
	

}
