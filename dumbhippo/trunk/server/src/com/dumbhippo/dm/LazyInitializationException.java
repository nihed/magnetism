package com.dumbhippo.dm;

import com.dumbhippo.server.NotFoundException;

public class LazyInitializationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public LazyInitializationException(String message, NotFoundException cause) {
		super(message, cause);
		
		if (cause == null)
			throw new RuntimeException("Cause must be specified");
	}
	
	@Override
	public NotFoundException getCause() {
		return (NotFoundException)super.getCause();
	}
}
