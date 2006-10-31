package com.dumbhippo.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

/**
 * This class implements the handling of singleton-per-destination producers and consumers
 * which is common between TxJmsSession and NonTxJmsSession. 
 */
abstract class AbstractJmsSession implements JmsSession {
	private Session baseSession;
	public Map<Destination, MessageProducer> producers = new HashMap<Destination, MessageProducer>();
	public Map<Destination, MessageConsumer> consumers = new HashMap<Destination, MessageConsumer>();
	
	protected AbstractJmsSession(Session baseSession) {
		this.baseSession = baseSession;
	}

	public MessageProducer getProducer(Destination destination) throws JMSException {
		MessageProducer producer = producers.get(destination);
		if (producer == null) {
			producer = getBaseSession().createProducer(destination);
			producers.put(destination, producer);
		}
		
		return producer;
	}

	public MessageConsumer getConsumer(Destination destination) throws JMSException {
		MessageConsumer consumer = consumers.get(destination);
		if (consumer == null) {
			consumer = getBaseSession().createConsumer(destination);
			consumers.put(destination, consumer);
		}
		
		return consumer;
	}
	
	public ObjectMessage createObjectMessage(java.io.Serializable object) throws JMSException {
		return getBaseSession().createObjectMessage(object);
	}
	
	protected void closeDestinations() throws JMSException {
		for (MessageProducer producer : producers.values())
			producer.close();
		
		for (MessageConsumer consumer : consumers.values())
			consumer.close();
	}

	protected Session getBaseSession() {
		return baseSession;
	}
}
