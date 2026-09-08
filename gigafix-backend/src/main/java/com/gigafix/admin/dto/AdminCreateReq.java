package com.gigafix.admin.dto;

import com.gigafix.admin.entity.AdminAccount.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AdminCreateReq(
		@NotBlank(message = "password不可為空")
	    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼需至少8碼，並包含大小寫英文字母及數字")
		String password,
		@NotBlank(message = "admin名稱不可為空")
		@Size(max = 20, message = "名稱字數上限為20個")
		String adminName,
		@NotNull(message = "admin權限不可為空")
		Role role
		) {

}
