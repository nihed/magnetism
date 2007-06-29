package com.dumbhippo.jive;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError.Condition;

public class RecentPostsIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public RecentPostsIQHandler() {
		super("DumbHippo Recent Posts IQ Handler");
		
		Log.debug("creating Hotness handler");
		info = new IQHandlerInfo("recentPosts", "http://dumbhippo.com/protocol/post");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		IQ reply = IQ.createResultIQ(packet);
		reply.setError(Condition.item_not_found);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
