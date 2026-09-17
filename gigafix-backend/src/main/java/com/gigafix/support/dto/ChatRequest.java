package com.gigafix.support.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
		@NotBlank(message = "訊息不可為空") @Size(max = 1000, message = "訊息不可超過1000字") String message,
		// 前端純前端暫存的對話紀錄，不是伺服器權威資料，service 層還會再檢查一次筆數/角色，不能只信這裡
		List<ChatTurnDto> history) {
}
