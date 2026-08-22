package com.gigafix.repair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuatationRequest {
// 技師填寫、修改檢測報價用
	
	@NotNull(message = "請提供技師ID，用來確認是不是本人認領的維修單")
	private Integer technicianId;
	
	@Size(max = 500)
	private String inspectionResult;

	@Size(max = 500)
	private String repairItems;

	private Integer estimatedCost;

}
