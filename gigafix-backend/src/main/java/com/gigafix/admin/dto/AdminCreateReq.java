package com.gigafix.admin.dto;

import com.gigafix.admin.entity.Role;

import lombok.Builder;

@Builder
public record AdminCreateReq(
		Integer adminId,
		String password,
		String adminName,
		Role adminRole
		) {

}
