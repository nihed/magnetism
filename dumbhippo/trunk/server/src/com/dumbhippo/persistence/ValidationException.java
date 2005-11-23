package com.dumbhippo.persistence;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class ValidationException extends Exception {
	private static final long serialVersionUID = 0L;
	
	public ValidationException(String message) {
		super(message);
	}
}
