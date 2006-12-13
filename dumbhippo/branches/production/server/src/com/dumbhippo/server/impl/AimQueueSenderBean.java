package com.dumbhippo.server.impl;

import org.jboss.annotation.ejb.Service;

import com.dumbhippo.botcom.BotTask;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.server.AimQueueSender;
import com.dumbhippo.server.SimpleServiceMBean;

@Service
public class AimQueueSenderBean implements AimQueueSender, SimpleServiceMBean {
	private JmsProducer producer;

	public void sendMessage(BotTaskMessage message) {
		producer.sendObjectMessage(message);

	}

	public void start() {
		producer = new JmsProducer(BotTask.QUEUE_NAME, JmsConnectionType.TRANSACTED_IN_SERVER);

	}

	public void stop() {
		producer.close();
		producer = null;
	}
}
