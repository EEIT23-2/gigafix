package com.gigafix.forum.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 討論區模組的自訂例外：自己帶 HTTP 狀態碼與錯誤代碼
 *
 * 為什麼需要它：forum 各 service 原本一律丟 JDK 的 IllegalArgumentException／IllegalStateException，
 * 由 ForumExceptionHandler 統一對應成 404／409。但「標題與內文不能為空」這類**參數驗證錯誤**
 * 也是丟 IllegalArgumentException，會被錯誤地對應成 404 Not Found——使用者把標題留空按發布，
 * 收到的卻是「找不到」，語意完全不對。需要明確指定狀態碼的情境就改用這個類別。
 *
 * 慣例比照 member／admin 模組既有的 MemberException、AdminAccountException。
 */
@Getter
public class ForumException extends RuntimeException {

	private final String errorCode;
	private final HttpStatus httpStatus;

	public ForumException(String errorCode, String message, HttpStatus httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	// 參數驗證失敗（400）。用靜態工廠讓呼叫端保持一行：throw ForumException.badRequest("...")
	public static ForumException badRequest(String message) {
		return new ForumException("FORUM_BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
	}

	// 檔案格式不支援（415）。目前用於圖片上傳的檔頭特徵碼（magic number）驗證失敗
	public static ForumException unsupportedMediaType(String message) {
		return new ForumException("FORUM_UNSUPPORTED_MEDIA_TYPE", message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	// 上游服務（Cloudinary）連線或驗證失敗（502）。不是使用者的錯，也不是我們的程式壞掉，
	// 用 502 而不是 500，讓前端訊息與維運判讀都能一眼看出是外部服務的問題
	public static ForumException badGateway(String message) {
		return new ForumException("FORUM_BAD_GATEWAY", message, HttpStatus.BAD_GATEWAY);
	}

	// 執行環境沒設定好（503）。與 502 刻意分開：502 是連得上但失敗，503 是根本沒有憑證可以連
	public static ForumException serviceUnavailable(String message) {
		return new ForumException("FORUM_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
	}
}
