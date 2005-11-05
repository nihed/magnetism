package com.dumbhippo.aimbot;

import javax.jms.JMSException;

public class JmsUncheckedException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	JmsUncheckedException(JMSException e) {
		super(e);
	}
	
	JMSException getJmsException() {
		return (JMSException) getCause();
	}
}
