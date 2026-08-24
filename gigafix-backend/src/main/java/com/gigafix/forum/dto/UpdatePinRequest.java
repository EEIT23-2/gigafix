package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Request：後台切換文章置頂狀態用
@Getter
@Setter
public class UpdatePinRequest {

	@NotNull(message = "置頂狀態不能為空")
	private Boolean isPinned;
}
