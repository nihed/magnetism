package com.dumbhippo.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

/**
 * This interface represents a small subset of JmsSession functionality that we
 * need in our application, with addition of the idea of singleton per-destination
 * producers and consumers. (Rather than using a subset, we could extend the full
 * Session interface, but that would be a lot more work to implement, and we don't
 * need most of it.)
 * 
 * @author otaylor
 */
interface JmsSession {
	MessageProducer getProducer(Destination destination) throws JMSException;
	MessageConsumer getConsumer(Destination destination) throws JMSException;
	
	ObjectMessage createObjectMessage(java.io.Serializable object) throws JMSException;
	void close() throws JMSException;
}
