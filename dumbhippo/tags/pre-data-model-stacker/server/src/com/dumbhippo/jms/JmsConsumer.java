package com.dumbhippo.jms;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * This class provides a simple robust way of fetching messages from a JMS
 * queue or topic; it internally creates Connection and Session objects and
 * if it gets an exception reading from a Connection will close the connection
 * and try again to connect, sleeping between connection attempts.
 * 
 * You would generally use this class inside a thread that reads messages
 * and processes them. (Possible enhancement: have a generic class that handles
 * the thread management and calls an interface for received messages.)  
 * 
 * Note that messages will be acknowledged before they are returned to you,
 * so if you fail to process a message it will not be redelivered. (This
 * has upsides and downsides; you lose the ability to handle intermittant
 * failure, but you don't get your logs chocked with debug spew if the
 * error occurs on every delivery.)
 * 
 * There is no reason to use this inside the app server, just create a 
 * message-driven bean.
 * 
 * @author hp
 * @author otaylor
 */
public class JmsConsumer extends JmsDestination {
	private static final Logger logger = GlobalSetup.getLogger(JmsConsumer.class);

	JmsSession session;

	/**
	 * Create a JmsConsumer for the specified destination
	 * 
	 * @param destinationName name of the queue or topic to receive messages from.
	 * @param connectionType the type of connection to the server to create. Transactional
	 *   connections are disallowed since they don't really make sense in our mode
	 *   of operation where we keep a single session open for the life of the
	 *   thread (which is likely the life of the server process.)
	 */
	public JmsConsumer(String destinationName, JmsConnectionType connectionType) {
		super(destinationName, connectionType);
		if (connectionType.isTransacted())
			throw new IllegalArgumentException("JmsConsumer can't be used with transactions");
	}
	
	/**
	 * Fetch a single message from the queue and return it. Blocks until a message
	 * is received.
	 * 
	 * @return the received message
	 */
	public Message receive() throws JmsShutdownException {
		while (true) {
			try {
				if (session == null) {
					session = createSession();
				}
				Message message = session.getConsumer(getDestination()).receive();
				if (message != null)
					return message;
				
				// If the message is null, our connection was closed. The retry
				// will throw JmsShutdownException() if it was closed via a
				// call to shutdown()
				session = null;
				
			} catch (JMSException e) {
				logger.warn("Got exception waiting for JMS message", e);
				closeOnFailure(); // will close session as a side-effect
				session = null;
			} 
		}
	}
}
