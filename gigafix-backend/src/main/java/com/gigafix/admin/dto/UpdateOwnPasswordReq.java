package com.gigafix.admin.dto;

public record UpdateOwnPasswordReq(
		String oldPassword,
		String newPassword		
		) {

}
