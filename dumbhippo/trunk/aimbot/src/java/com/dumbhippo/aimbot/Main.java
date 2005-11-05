package com.dumbhippo.aimbot;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class Main {

	private static void testJms() {
		JmsProducer producer = new JmsProducer("FooQueue");
		JmsConsumer consumer = new JmsConsumer("FooQueue");
		
		TextMessage message = producer.createTextMessage("WOOOHOO");
		producer.send(message);
		
		System.out.println("waiting to receive...");
		
		Message received = consumer.receive();
		TextMessage textReceived = (TextMessage) received;
		
		try {
			System.out.println("Got: " + textReceived.getText());
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		producer.close();
		consumer.close();
	}
	
	public static void main(String[] args) {
		
		Bot bot = new Bot();
		Thread t = new Thread(bot);
		t.setDaemon(true);
		t.start();
		
		// the Bot is a daemon thread; here we 
		// just want to wait forever until killed by 
		// the OS. This means when we're killed by the 
		// OS the JVM will exit.
		while (true) {
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
			}
		}
	}
	
}
