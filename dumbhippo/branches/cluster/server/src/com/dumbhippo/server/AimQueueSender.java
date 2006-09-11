package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.botcom.BotTaskMessage;

@Local
public interface AimQueueSender {
	void sendMessage(BotTaskMessage message);
}
