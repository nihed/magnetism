package com.dumbhippo.search;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.hibernate.search.backend.LuceneWork;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsConsumer;
import com.dumbhippo.jms.JmsShutdownException;
import com.dumbhippo.server.util.EJBUtil;

/**
 * Cluster global singleton that takes tasks from the Indexer queue and dispatches them
 * to the individual dispatchers. All the dispatching is asynchronous so the work we
 * do for each message is very small. A disadvantage of this extra layer of asynchronicity
 * is that we'll dequeue a message even if we fail to index it, but does give us the
 * right level of parallelization ... one indexing thread per index. Generally, failing
 * to index an object isn't a big deal.
 * 
 * We actually create one of these service MBeans in each server in the cluster, but only
 * one is active at once; we configure the JBoss HASingletonController to call our
 * startSingleton()/stopSingleton() methods as appropriate. 
 * 
 *  @author otaylor
 */
public class IndexerService extends ServiceMBeanSupport implements IndexerServiceMBean {
	private static final Logger logger = GlobalSetup.getLogger(IndexerService.class);
	
	public static final String QUEUE_NAME = "queue/IndexQueue";

	private JmsConsumer consumer;
	private Thread consumerThread;
	
	@Override
	protected void startService() {
	}
	
	@Override
	protected void stopService() {
		stopQueueConsumer();
	}
	
	public void startSingleton() {
		startQueueConsumer();
	}
	
	public void stopSingleton() {
		stopQueueConsumer();
	}
	
	private void startQueueConsumer() {
		if (consumer != null)
			return;
		
		logger.info("Starting IndexQueue consumer thread");
		
		consumer = new JmsConsumer(QUEUE_NAME, JmsConnectionType.NONTRANSACTED_IN_SERVER);
		consumerThread = new Thread(new QueueConsumer(), "IndexQueueConsumer");
		consumerThread.start(); 
	}
	
	private void stopQueueConsumer() {
		if (consumer == null)
			return;
		
		logger.info("Stopping IndexQueue consumer thread");

		consumer.shutdown(); // Will stop Queue consumer thread as a side effect
		
		try {
			consumerThread.join(30 * 1000);
			if (consumerThread.isAlive())
				logger.warn("Timed out waiting for IndexQueue consumer thread to shut down");
		} catch (InterruptedException e) {
			// Shouldn't happen
		}
		
		consumer = null;
		consumerThread = null;
	}
	
	private class QueueConsumer implements Runnable {
		public void run() {
			while (true) {
				try {
					Message message = consumer.receive();
					if (!(message instanceof ObjectMessage)) {
						logger.warn("Got unexpected type of message in queue.");
						continue;
					}
					
					Object object;
					try {
						object = ((ObjectMessage)message).getObject();
					} catch (JMSException e) {
						logger.warn("Error retrieving object from queue.", e);
						continue;
					}
					
					SearchSystem searchSystem = EJBUtil.defaultLookup(SearchSystem.class);
					
					/**
					 * We have two different types of message to send over the queue
					 * 
					 *  A) Perform the LuceneWork indices in this List
					 *  B) Clear the index for this Class
					 *  
					 *  Rather than getting fancy and wrapping things up in a nice
					 *  message object, we just use instanceof to distinguish the two.
					 */
					
					if (object instanceof List<?>) {
						searchSystem.doIndexWork(TypeUtils.castList(LuceneWork.class, (List<?>)object));
					} else if (object instanceof Class<?>) {
						searchSystem.clearIndex((Class<?>)object);
					} else {
						logger.warn("Got unexpected type of object in indexing queue.");
						continue;
					}
	
				} catch (JmsShutdownException e) {
					logger.debug("Queue was shut down, exiting thread");
					break;
				} catch (RuntimeException e) {
					logger.error("Unexpected error receiving dispatching messages", e);
				}
			}
		}
	}
}
