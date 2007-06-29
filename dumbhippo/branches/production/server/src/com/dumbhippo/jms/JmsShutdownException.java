package com.dumbhippo.jms;

/**
 * Thrown on an attempt to access a JmsConsumer or JmsProducer that has explicitly
 * been shutdown.
 *  
 * @author otaylor
 */
public class JmsShutdownException extends Exception {
	private static final long serialVersionUID = 1L;

}
