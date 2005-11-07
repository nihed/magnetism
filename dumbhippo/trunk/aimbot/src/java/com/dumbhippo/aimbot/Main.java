package com.dumbhippo.aimbot;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

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
			JmsConsumer consumer = new JmsConsumer(queue, false);
			
			while (true) {
		
				if (this.quit) {
					logger.info("Exiting queue dispatcher for queue " + queue);
					break;
				}
				
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
		t.start();
		return t;
	}
	
	public static void main(String[] args) {
		BotPool pool = new BotPool();
		
		Thread t = watchQueue("FooQueue", pool);

		generateTestTasks("FooQueue");
		
		while (t.isAlive()) {
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		}
		logger.info("Exiting");
	}
}
