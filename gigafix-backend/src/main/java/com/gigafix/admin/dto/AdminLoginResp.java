package com.gigafix.admin.dto;

import com.gigafix.admin.entity.AdminAccount.Role;

import lombok.Builder;

@Builder
public record AdminLoginResp(
		String name,
		Role role
		) {
}
