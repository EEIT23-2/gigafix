package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuperAdminSetupReq(
	    @NotBlank(message = "帳號名稱不可為空")
	    String name,
	    @NotBlank(message = "密碼不可為空")
	    @Size(min = 8, message = "密碼長度至少 8 碼")
	    String password
	) {}