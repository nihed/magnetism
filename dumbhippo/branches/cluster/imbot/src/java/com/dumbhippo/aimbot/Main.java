package com.dumbhippo.aimbot;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.botcom.BotEvent;
import com.dumbhippo.botcom.BotEventLogin;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotTask;
import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsConsumer;
import com.dumbhippo.jms.JmsProducer;

public class Main {
	private static Logger logger = GlobalSetup.getLogger(Main.class);
	
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
			JmsProducer producer = new JmsProducer(queue, JmsConnectionType.NONTRANSACTED_IN_CLIENT);
			
			try {
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
						
						logger.debug("{}", event);
						
						if ((event instanceof BotEventToken) ||
						    (event instanceof BotEventLogin)) {
							
							logger.debug("Sending event type " + event.getClass().getName());
							
							producer.sendObjectMessage(event);
						}
						
					} catch (InterruptedException e) {
						// will check the quit flag now
					}
				}
			} finally {
				producer.close();
			}
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
			JmsConsumer consumer = new JmsConsumer(queue, JmsConnectionType.NONTRANSACTED_IN_CLIENT);
			
			try {
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
			} finally {
				consumer.close();
			}
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
		
		// init log4j, if we weren't lazy we might load it from a config file, 
		// but we are lazy
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
		
		// now start up our daemon
		
		BotPool pool = new BotPool();
		
		// outgoing queue is outgoing from jboss, incoming goes to jboss
		Thread queueWatcher = watchQueue(BotTask.QUEUE_NAME, pool);
		Thread eventWatcher = watchEvents(BotEvent.QUEUE_NAME, pool);
		
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
