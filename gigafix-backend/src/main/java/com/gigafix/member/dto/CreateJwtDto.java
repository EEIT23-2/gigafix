package com.gigafix.member.dto;

import lombok.Builder;

@Builder
public record CreateJwtDto(String subject,
							String username) {

}
