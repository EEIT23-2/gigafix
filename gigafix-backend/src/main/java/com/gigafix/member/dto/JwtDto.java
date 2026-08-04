package com.gigafix.member.dto;

import lombok.Builder;

@Builder
public record JwtDto(Long subject, String username) {
	//user id 就是subject
}
