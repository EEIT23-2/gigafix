package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOwnPasswordReq(
		@NotBlank(message = "password不可為空")
	    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼需至少8碼，並包含大小寫英文字母及數字")
		String oldPassword,
		@NotBlank(message = "password不可為空")
	    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼需至少8碼，並包含大小寫英文字母及數字")
		String newPassword
		) {

}
