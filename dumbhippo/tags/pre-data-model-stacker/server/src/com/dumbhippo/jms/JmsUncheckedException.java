package com.dumbhippo.jms;

import javax.jms.JMSException;


/**
 * Wraps the JMSException checked exception with an unchecked exception.
 * 
 * @author hp
 */
public class JmsUncheckedException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	JmsUncheckedException(JMSException e) {
		super(e);
	}
	
	JMSException getJmsException() {
		return (JMSException) getCause();
	}
}
