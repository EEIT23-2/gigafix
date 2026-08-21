package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Request：建立分類所需要的資訊
@Getter
@Setter
public class CreateCategoryRequest {

	@NotBlank(message = "分類名稱不能為空")
	@Size(max = 60, message = "分類名稱不能超過60字")
	private String name;
}
