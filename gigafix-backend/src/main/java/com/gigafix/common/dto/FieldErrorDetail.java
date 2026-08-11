package com.gigafix.common.dto;

import org.springframework.web.bind.MethodArgumentNotValidException;

import lombok.Builder;

@Builder
public record FieldErrorDetail(
		String field,
        String message,
        Object rejectedValue
		) {}
//這個dto只有在@Valid偵查到不符合自訂義規定而拋MethodArgumentNotValidException時，才有辦法撈到東西