package com.gigafix.repair.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) //404
public class RepairNotFoundException extends RuntimeException {

	public RepairNotFoundException(String message) {
		super(message);
	}

}