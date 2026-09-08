package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Request：編輯樓層只會改內文。標題（樓主標題+樓層數）、分類、樓層數都由後端擁有，
// 不開放前端傳入，否則等於把伺服器自己產生的欄位交給呼叫者決定
@Getter
@Setter
public class UpdateFloorRequest {

	@NotBlank(message = "內文不能為空")
	private String content;
}
