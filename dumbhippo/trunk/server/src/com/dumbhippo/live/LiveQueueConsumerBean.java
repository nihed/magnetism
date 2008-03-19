package com.dumbhippo.live;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.dm.ChangeNotificationSet;
import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsConsumer;
import com.dumbhippo.jms.JmsShutdownException;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

//
// Handles taking events queued via LiveState.queueUpdate and dispatching
// them to the appropriate "processor bean"
//

@Service
public class LiveQueueConsumerBean implements SimpleServiceMBean {
	
	@Resource SessionContext context;

	static private final Logger logger = GlobalSetup.getLogger(LiveQueueConsumerBean.class);
	
	private JmsConsumer consumer;
	private Thread consumerThread;

	public void start() {
		consumer = new JmsConsumer(LiveEvent.TOPIC_NAME, JmsConnectionType.NONTRANSACTED_IN_SERVER);
		consumerThread = new Thread(new LiveTopicConsumer(), "LiveTopicConsumer");
		consumerThread.start(); 
	}

	public void stop() {
		consumer.close(); // Will stop consumer thread as a side effect
		consumer = null;
	}

	private void process(LiveEvent event, boolean isLocal) {
		// To find the right "processor bean" for this event, we have the 
		// processor beans register themselves in JDNI under their class name
		// using the JBoss @LocalBinding EJB3-annotation extension. There
		// is no standard way of finding a bean at runtime.
		
		Class<? extends LiveEventProcessor> clazz = event.getProcessorClass();
		if (clazz != null) {
			LiveEventProcessor processor = (LiveEventProcessor)context.lookup(clazz.getCanonicalName());
			if (processor == null) {
				logger.warn("Could not lookup event processor bean " + clazz.getCanonicalName());
			} else {
				processor.process(LiveState.getInstance(), event, isLocal);
			}
		}
		
		LiveState.getInstance().invokeEventListeners(event);
	}

	private void handleMessage(ObjectMessage message) {
		Object obj;
		String sourceAddress;
		try {
			obj = ((message).getObject());
			sourceAddress = message.getStringProperty("sourceAddress");
		} catch (JMSException e) {
			logger.warn("Error retrieving object from queue.", e);
			return;
		}
		
		String localAddress = System.getProperty("jboss.bind.address");
		boolean isLocal = localAddress.equals(sourceAddress);
		
		logger.debug("Got object in " + LiveEvent.TOPIC_NAME + ": " + obj + " (isLocal=" + isLocal + ")");
		
		if (obj instanceof LiveEvent) {
			process((LiveEvent) obj, isLocal);
		} else if (obj instanceof ChangeNotificationSet) {
			if (!isLocal)
				DataService.getModel().notifyRemoteChange((ChangeNotificationSet)obj);
		} else {
			logger.warn("Got unknown object: " + obj);
		}
	}
	
	private class LiveTopicConsumer implements Runnable {
		public void run() {
			while (true) {
				try {
					final Message message = consumer.receive();
					if (!(message instanceof ObjectMessage)) {
						logger.warn("Got unexpected type of message in queue.");
						continue;
					}
					TxUtils.runInTransaction(new TxRunnable() {
						public void run() throws RetryException {
							// Any database work should have been done on the sending side before sending the
							// message. Here we are just updating transient state and notifying.
							DataService.getModel().initializeReadOnlySession(AnonymousViewpoint.getInstance(Site.NONE));
							handleMessage((ObjectMessage)message);
						}
					});
				} catch (JmsShutdownException e) {
					logger.debug("Queue was shut down, exiting thread");
					break;
				} catch (RuntimeException e) {
					logger.error("Unexpected error receiving live topic messages", e);
				}
			}
		}
	}
}
