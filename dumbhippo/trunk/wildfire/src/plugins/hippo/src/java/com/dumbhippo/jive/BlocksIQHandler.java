package com.dumbhippo.jive;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

public class BlocksIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public BlocksIQHandler() {
		super("Hippo blocks IQ Handler");
		Log.debug("creating BlocksIQHandler");
		info = new IQHandlerInfo("blocks", "http://dumbhippo.com/protocol/blocks");
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		Log.debug("handling IQ packet " + packet);
		IQ reply = IQ.createResultIQ(packet);
		// Element iq = packet.getChildElement();
		JID from = packet.getFrom();
		
		MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
		Element childElement = XmlParser.elementFromXml(glue.getBlocksXml(from.getNode(), 0 /* timestamp */,
				0 /* start */, 10 /* count */));		

		reply.setChildElement(childElement);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}

}
