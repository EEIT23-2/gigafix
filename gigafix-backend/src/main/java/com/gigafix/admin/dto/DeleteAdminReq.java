package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAdminReq(
		@NotBlank(message = "admin id不可為空")
		Integer adminId
		) {

}
