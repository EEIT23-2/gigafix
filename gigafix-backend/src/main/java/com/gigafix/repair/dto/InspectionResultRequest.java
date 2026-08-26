package com.gigafix.repair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InspectionResultRequest {
// 技師報價送出後，補充/更新檢測備註用（例如維修中發現新問題、電話聯絡客戶溝通紀錄）
// 報價項目跟報價 送出後就不能再更動，僅能更動 檢測結果，如有增加維修項目，價格會直接在finalCost加上去

	@NotNull(message = "請提供技師ID，用來確認是不是本人負責的維修單")
	private Integer technicianId;

	@NotNull(message = "請填寫要更新的內容")
	@Size(max = 500)
	private String inspectionResult;

}