package com.gigafix.admin.dto;

import com.gigafix.admin.entity.AdminAccount.Role;

public record UpdateRoleReq(
		Role role,
		Integer id
		) {

}
