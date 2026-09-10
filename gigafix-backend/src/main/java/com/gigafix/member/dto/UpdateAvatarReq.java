package com.gigafix.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAvatarReq(
        @NotBlank(message = "頭像網址不可為空") @Pattern(regexp = "^https?://.+", message = "頭像格式錯誤，需為http(s)開頭的網址") String profileImageUrl) {
}