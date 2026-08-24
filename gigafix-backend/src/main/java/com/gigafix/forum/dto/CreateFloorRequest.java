package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Request：蓋樓所需要的資訊（標題/分類/狀態皆由後端依根文章推導，不需前端提供）
@Getter
@Setter
public class CreateFloorRequest {

	@NotBlank(message = "內文不能為空")
	private String content;
}
