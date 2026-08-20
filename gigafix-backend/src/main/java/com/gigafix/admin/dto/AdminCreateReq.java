package com.gigafix.admin.dto;

import com.gigafix.admin.entity.AdminAccount.Role;

import lombok.Builder;

@Builder
public record AdminCreateReq(
		String password,
		String adminName,
		Role role
		) {

}
