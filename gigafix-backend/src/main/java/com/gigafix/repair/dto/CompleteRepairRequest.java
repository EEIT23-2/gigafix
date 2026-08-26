package com.gigafix.repair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteRepairRequest {
// 維修完成

	@NotNull(message = "請提供技師ID，用來確認是不是本人負責的維修單")
	private Integer technicianId;

	// 不傳就代表沿用 estimatedCost，不用特別做什麼
	private Integer finalCost;

	// 只有 finalCost 跟 estimatedCost 不同時才必填，由 Service 層判斷，不用 @NotNull
	@Size(max = 500)
	private String adjustmentNote;

}