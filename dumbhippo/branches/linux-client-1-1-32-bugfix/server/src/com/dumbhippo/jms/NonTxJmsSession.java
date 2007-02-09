package com.dumbhippo.jms;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Version of JmsSession used in the non-transactional case, where
 * there is one underlying session object for each JmsSession. 
 *  
 * @author otaylor
 */
class NonTxJmsSession extends AbstractJmsSession {
	public NonTxJmsSession(Session baseSession) throws JMSException {
		super(baseSession);
	}

	public void close() throws JMSException {
		closeDestinations();
		getBaseSession().close();
	}
}
