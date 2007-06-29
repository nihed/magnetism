package com.dumbhippo.jive;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;

public class MySpaceIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public MySpaceIQHandler() {
		super("DumbHippo MySpace IQ Handler");
		Log.debug("creating MySpace IQ handler");
		info = new IQHandlerInfo("myspace", "http://dumbhippo.com/protocol/myspace");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		IQ reply = IQ.createResultIQ(packet);

		makeError(reply, "MySpace IQ handling disabled");
		return reply;		
	}
	
	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
