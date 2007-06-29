package com.dumbhippo.jive;

import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;
import org.jivesoftware.wildfire.handler.IQHandler;

public abstract class AbstractIQHandler extends IQHandler {

	protected AbstractIQHandler(String moduleName) {
		super(moduleName);
	}

	protected void makeError(IQ reply, String message) {
		Log.error(message);
		reply.setError(new PacketError(PacketError.Condition.bad_request, 
                					   PacketError.Type.modify, 
                					   message));
		return;
	}
	
}
