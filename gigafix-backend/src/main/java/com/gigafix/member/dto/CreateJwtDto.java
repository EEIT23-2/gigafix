package com.gigafix.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder //要自己在手動組裝dto作為參數給出jwt util的方法
public record CreateJwtDto(@NotBlank(message = "member id不可為空") String subject,
							@NotBlank(message = "member name不可為空") String membername) {

}
