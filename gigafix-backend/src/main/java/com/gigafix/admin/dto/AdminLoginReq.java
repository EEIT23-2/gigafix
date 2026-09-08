package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminLoginReq(
		@NotBlank(message = "admin名稱不可為空")
		@Size(max = 20, message = "名稱字數上限為20個字元")
		String adminName,
		@NotBlank(message = "password不可為空")
	    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼需至少8碼，並包含大小寫英文字母及數字")
		String password
		) {

}
