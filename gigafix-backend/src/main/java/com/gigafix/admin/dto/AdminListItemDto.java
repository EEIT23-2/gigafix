package com.gigafix.admin.dto;


import java.time.LocalDateTime;

import com.gigafix.admin.entity.AdminAccount.Role;

import lombok.Builder;

// 專門給「查詢所有管理員」這支列表API用的response，跟AdminInfoDto分開是因為online這個欄位
// 只有列表頁需要顯示在線狀態，其他共用AdminInfoDto的操作(建立、改密碼、改角色等)都不需要這個欄位
@Builder
public record AdminListItemDto(
		Integer adminId,
		String adminName,
		Role role,
		LocalDateTime createDateTime,
		boolean online
		) {

}
