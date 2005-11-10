package com.dumbhippo.aimbot;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.botcom.BotEvent;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotTask;
import com.dumbhippo.botcom.BotTaskInvite;
import com.dumbhippo.jms.JmsConsumer;
import com.dumbhippo.jms.JmsProducer;

public class Main {
	private static Log logger = GlobalSetup.getLog(Main.class);

	private static void generateTestTasks(String queue) {
		logger.debug("Sending test tasks");
		
		JmsProducer producer = new JmsProducer("FooQueue", false);
		
		BotTaskInvite invite = new BotTaskInvite("http://inviteurl", "Some Guy", "someguy21", "hp40000");
		ObjectMessage message = producer.createObjectMessage(invite);
		producer.send(message);
		
		producer.close();
		
		logger.debug("Done sending test tasks");
	}
	
	static class PoolToQueueDispatcher implements Runnable {
		private String queue;
		private BotPool pool;
		private boolean quit;
		
		PoolToQueueDispatcher(String queue, BotPool pool) {
			this.queue = queue;
			this.pool = pool;
			this.quit = false;
		}
		
		public void quit() {
			quit = true;
			// FIXME would have to wake up the thread someway to really use this
		}
		
		public void run() {
			JmsProducer producer = new JmsProducer(queue, false);
			
			logger.info("Starting event reader thread for queue " + queue);
			
			while (true) {
				
				if (this.quit) {
					logger.info("Exiting queue dispatcher for queue " + queue);
					break;
				}
				
				logger.debug("Waiting for events from bot pool");
				BotEvent event;
				try {
					event = pool.take();
					
					logger.debug(event);
					
					if (event instanceof BotEventToken) {
						logger.debug("Sending token event");
						
						TextMessage message = producer.createTextMessage(event.toString());
						producer.send(message);
					}
					
				} catch (InterruptedException e) {
					// will check the quit flag now
				}
			}
			
			producer.close();
		}
	}
	
	static class QueueToPoolDispatcher implements Runnable {
		private String queue;
		private BotPool pool;
		private boolean quit;
		
		QueueToPoolDispatcher(String queue, BotPool pool) {
			this.queue = queue;
			this.pool = pool;
			this.quit = false;
		}

		public void quit() {
			quit = true;
			// FIXME would have to wake up the thread someway to really use this
		}
		
		public void run() {
			JmsConsumer consumer = new JmsConsumer(queue);
			
			logger.info("Starting dispatch thread for queue " + queue);
			
			while (true) {
		
				if (this.quit) {
					logger.info("Exiting queue dispatcher for queue " + queue);
					break;
				}
				
				logger.debug("Waiting for message in queue " + queue);
				Message received = consumer.receive();
				
				logger.debug("received message " + received);
				
				if (received instanceof ObjectMessage) {
					
					ObjectMessage objectReceived = (ObjectMessage) received;
					
					Object obj;
					
					try {
						 obj = objectReceived.getObject();
					} catch (JMSException e) {
						e.printStackTrace();
						continue; // not much else to do...
					}
					
					logger.debug("Message contained object: " + obj.getClass().getCanonicalName());
					
					if (obj instanceof BotTask) {
						pool.put((BotTask) obj);
					}
				}
			}

			consumer.close();
		}		
	}
	
	private static Thread watchQueue(String queue, BotPool pool) {
		logger.debug("Connecting queue " + queue + " to the bot pool");
		Thread t = new Thread(new QueueToPoolDispatcher(queue, pool));
		t.setDaemon(true);
		t.setName("Dispatch-" + queue);
		t.start();
		return t;
	}
	
	private static Thread watchEvents(String queue, BotPool pool) {
		logger.debug("Connecting bot pool events to queue " + queue);
		Thread t = new Thread(new PoolToQueueDispatcher(queue, pool));
		t.setDaemon(true);
		t.setName("Events-" + queue);
		t.start();
		return t;
	}
	
	public static void main(String[] args) {
		BotPool pool = new BotPool();
		
		// outgoing queue is outgoing from jboss, incoming goes to jboss
		Thread queueWatcher = watchQueue("OutgoingAimQueue", pool);
		Thread eventWatcher = watchEvents("IncomingAimQueue", pool);
		
		pool.start();
		
		//generateTestTasks("FooQueue");
		
		// this is sort of broken, in practice we're never going to exit
		while (queueWatcher.isAlive()) {
			try {
				queueWatcher.join();
				eventWatcher.interrupt();
			} catch (InterruptedException e) {
			}
		}
		while (eventWatcher.isAlive()) {
			try {
				eventWatcher.join();
			} catch (InterruptedException e) {
			}
		}
		
		logger.info("Exiting");
		System.exit(0);
	}
}
