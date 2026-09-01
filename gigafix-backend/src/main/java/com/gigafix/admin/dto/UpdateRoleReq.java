package com.gigafix.admin.dto;

import com.gigafix.admin.entity.AdminAccount.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleReq(
		@NotNull(message = "admin權限不可為空")
		Role role
		) {

}
