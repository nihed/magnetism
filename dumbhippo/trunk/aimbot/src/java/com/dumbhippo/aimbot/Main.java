package com.dumbhippo.aimbot;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public class Main {

	public static void main(String[] args) {
		
		InitialContext ctx = null;
		try {
			ctx = new InitialContext();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			NamingEnumeration<NameClassPair> ne = ctx.list("/");
			while (ne.hasMore()) {
				NameClassPair pair = ne.next();
				System.out.println(pair.getName() + ", " + pair.getClassName());
			}
		} catch (NamingException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		ConnectionFactory connectionFactory = null;
		try {
			connectionFactory = (ConnectionFactory) ctx.lookup("RMIConnectionFactory");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Connection connection = null;
		
		try {
			connection = connectionFactory.createConnection();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Session session = null;
		
		try {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Destination destination = null;
		try {
			destination = (Destination) ctx.lookup("queue/FooQueue");
		} catch (NamingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		MessageProducer messageProducer = null;
		
		try {
			messageProducer = session.createProducer(destination);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		TextMessage message = null;
		
		try {
			message = session.createTextMessage("WOOOHOO");
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			messageProducer.send(message);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		MessageConsumer messageConsumer = null;
		
		try {
			messageConsumer = session.createConsumer(destination);
		} catch (JMSException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		System.out.println("waiting to receive...");
		
		Message received = null;
		
		try {
			received = messageConsumer.receive();
		} catch (JMSException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		TextMessage textReceived = (TextMessage) received;
		
		try {
			System.out.println("Got: " + textReceived.getText());
		} catch (JMSException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		try {
			messageProducer.close();
			session.close();
			connection.close();
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		if (false && true)
			throw new RuntimeException("quit here");
		
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
