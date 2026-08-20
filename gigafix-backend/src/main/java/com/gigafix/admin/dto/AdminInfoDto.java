package com.gigafix.admin.dto;


import java.time.LocalDateTime;

import com.gigafix.admin.entity.AdminAccount.Role;

import lombok.Builder;

@Builder
public record AdminInfoDto(
		Integer adminId,
		String adminName,
		Role role,
		LocalDateTime createDateTime
		) {

}
