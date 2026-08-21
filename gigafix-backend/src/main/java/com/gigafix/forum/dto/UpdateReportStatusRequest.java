package com.gigafix.forum.dto;

import com.gigafix.forum.entity.Report;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Request：後台處理／關閉檢舉用
@Getter
@Setter
public class UpdateReportStatusRequest {

	@NotNull(message = "狀態不能為空")
	private Report.ReportStatus status;
}
