package com.gigafix.admin.dto;

import lombok.Builder;

@Builder
public record AdminLoginResp(
		String name,
		String roleName //要把Role裡面的字串拿出來給前端
		) {

}
