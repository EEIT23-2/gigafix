package com.gigafix.common.dto;

import lombok.Builder;

@Builder
public record FieldErrorDetail(
		String field,
        String message,
        Object rejectedValue
		) {}
