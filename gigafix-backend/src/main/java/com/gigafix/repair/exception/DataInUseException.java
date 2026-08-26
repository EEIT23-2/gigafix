package com.gigafix.repair.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) //409
public class DataInUseException extends RuntimeException {

	public DataInUseException(String message) {
		super(message);
	}

}