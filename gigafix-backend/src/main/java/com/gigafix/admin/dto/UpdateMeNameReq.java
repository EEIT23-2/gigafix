package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMeNameReq(
		@NotBlank(message = "admin名稱不可為空")
		String newName) {

}
