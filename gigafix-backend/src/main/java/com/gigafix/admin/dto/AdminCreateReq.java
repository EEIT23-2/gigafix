package com.gigafix.admin.dto;

import com.gigafix.admin.entity.AdminAccount.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AdminCreateReq(
		@NotBlank(message = "password不可為空")
	    @Size(min = 8, message = "密碼長度不可低於8個字元")
		String password,
		@NotBlank(message = "admin名稱不可為空")
		String adminName,
		@NotNull(message = "admin權限不可為空")
		Role role
		) {

}
