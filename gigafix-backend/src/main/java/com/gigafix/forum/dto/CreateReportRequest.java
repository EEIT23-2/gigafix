package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Request：檢舉所需要的資訊
@Getter
@Setter
public class CreateReportRequest {

	@NotBlank(message = "檢舉原因不能為空")
	@Size(max = 500, message = "檢舉原因不能超過500字")
	private String reason;
}
