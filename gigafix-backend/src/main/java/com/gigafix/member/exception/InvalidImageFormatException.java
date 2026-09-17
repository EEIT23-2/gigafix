package com.gigafix.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidImageFormatException extends MemberException {
    public InvalidImageFormatException() {
        super("INVALID_IMAGE_FORMAT", "圖片格式錯誤，僅支援.Jpg或.Png檔", HttpStatus.UNSUPPORTED_MEDIA_TYPE); // 415
    }
}
