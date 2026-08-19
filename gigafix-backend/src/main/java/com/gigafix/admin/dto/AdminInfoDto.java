package com.gigafix.admin.dto;


import java.time.LocalDateTime;


import lombok.Builder;

@Builder
public record AdminInfoDto(
		Integer adminId,
		String adminName,
		String adminRole,
		LocalDateTime createDateTime
		) {

}
