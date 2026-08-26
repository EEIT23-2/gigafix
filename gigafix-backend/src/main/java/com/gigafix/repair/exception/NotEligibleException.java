package com.gigafix.repair.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
 
@ResponseStatus(HttpStatus.FORBIDDEN) //403
public class NotEligibleException extends RuntimeException{
	
	public NotEligibleException(String message) {
		super(message);
	}
 
}