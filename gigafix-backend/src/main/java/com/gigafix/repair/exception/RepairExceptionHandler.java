package com.gigafix.repair.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gigafix.common.dto.ErrorResp;

/**
 * 維修模組專屬的例外處理器
 *
 * repair 的自訂例外只掛了 @ResponseStatus，錯誤訊息要靠 Spring Boot 預設錯誤回應帶出去。
 * 本機有 spring-boot-devtools（它會把 server.error.include-message 設成 always）所以看得到訊息，
 * 但雲端是打包後的 jar，devtools 不會進去，預設 never，前端就只剩「HTTP 409」。
 * 這裡明確把訊息放進 ErrorResp.message，本機／雲端行為一致。
 *
 * basePackages 限定在 com.gigafix.repair，不影響其他模組；狀態碼沿用各例外上 @ResponseStatus 標的值。
 */
@RestControllerAdvice(basePackages = "com.gigafix.repair")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RepairExceptionHandler {

	@ExceptionHandler({ InvalidRepairStatusException.class, TimeConflictException.class,
			DataInUseException.class, RepairNotFoundException.class,
			NotEligibleException.class, InvalidFileFormatException.class })
	public ResponseEntity<ErrorResp> handleRepairException(RuntimeException e) {
		ResponseStatus rs = AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
		HttpStatus status = rs != null ? rs.code() : HttpStatus.BAD_REQUEST;

		return ResponseEntity.status(status)
				.body(ErrorResp.builder()
						.message(e.getMessage())
						.build());
	}
}
