package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginReq(
		@NotBlank(message = "admin名稱不可為空")
		@Size(max = 20, message = "名稱字數上限為20個字元")
		String adminName,
		@NotBlank(message = "password不可為空")
	    @Size(min = 8, message = "密碼長度不可低於8個字元")
		String password
		) {

}
