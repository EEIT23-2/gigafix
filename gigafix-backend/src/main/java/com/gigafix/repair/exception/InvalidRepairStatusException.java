package com.gigafix.repair.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) //409
public class InvalidRepairStatusException extends RuntimeException{
	
	public InvalidRepairStatusException(String message) {
		super(message);
	}

}
