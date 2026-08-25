package com.gigafix.forum.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gigafix.common.dto.ErrorResp;

/**
 * 討論區模組專屬的例外處理器
 *
 * forum 各 service 目前一律丟 JDK 內建的 IllegalStateException／IllegalArgumentException，
 * 而 common 的 GlobalExceptionHandler 沒有對應的 handler，導致「分類名稱已存在」「此分類仍有文章使用中」
 * 這類使用者需要看到的訊息全部變成 HTTP 500 且內容不可見。這裡把它們轉成合理的狀態碼與既有的 ErrorResp 格式。
 *
 * basePackages 限定在 com.gigafix.forum，所以不會影響 order／cart／member 等其他模組的既有行為。
 * 與 GlobalExceptionHandler 沒有例外型別重疊（它處理的是 MethodArgumentNotValidException、
 * MemberException、AdminAccountException、AuthenticationException），參數驗證的 400 仍由它負責。
 */
@RestControllerAdvice(basePackages = "com.gigafix.forum")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ForumExceptionHandler {

	// 自訂例外：自己帶狀態碼，優先於下面兩個 JDK 例外的預設對應
	// 用於「不該被歸類成 404／409」的情境，目前是參數驗證失敗（400）
	@ExceptionHandler(ForumException.class)
	public ResponseEntity<ErrorResp> handleForumException(ForumException e) {

		return ResponseEntity.status(e.getHttpStatus())
				.body(ErrorResp.builder()
						.errorCode(e.getErrorCode())
						.message(e.getMessage())
						.build());
	}

	// 狀態衝突：名稱重複、分類仍有文章使用中、重複按讚、文章目前無法蓋樓、無權限操作等
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResp> handleIllegalState(IllegalStateException e) {

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResp.builder()
						.errorCode("FORUM_CONFLICT")
						.message(e.getMessage())
						.build());
	}

	// 查不到資料：分類／文章／留言／會員不存在
	// 注意：forum 各 service 目前只把 IllegalArgumentException 用在「不存在」的情境，
	// 所以這裡統一對應 404；若日後有其他語意的 IllegalArgumentException 要另外拆分
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResp> handleIllegalArgument(IllegalArgumentException e) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResp.builder()
						.errorCode("FORUM_NOT_FOUND")
						.message(e.getMessage())
						.build());
	}
}
