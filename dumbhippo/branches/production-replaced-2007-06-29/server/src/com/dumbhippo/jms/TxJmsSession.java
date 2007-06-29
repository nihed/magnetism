package com.dumbhippo.jms;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Version of JmsSession used in the transactional case, where we share
 * a single underlying session object between all JmsSession objects for the
 * same transaction.
 *  
 * @author otaylor
 */
class TxJmsSession extends AbstractJmsSession {
	public TxJmsSession(Session baseSession) throws JMSException {
		super(baseSession);
	}

	public void close() throws JMSException {
		closeDestinations();
		
		// We don't want to close the base session - that will be done when the 
		// transaction completes
	}
}
