package com.dumbhippo.jive;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.server.MessengerGlue;
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
		
		Element iq = packet.getChildElement();
		
        String lastTimestampStr = iq.attributeValue("lastTimestamp");
        if (lastTimestampStr == null) {
        	makeError(reply, "blocks IQ missing lastTimestamp attribute");
        	return reply;
        }
        
        long lastTimestamp;
        try {
        	lastTimestamp = Long.parseLong(lastTimestampStr);
        } catch (NumberFormatException e) {
        	makeError(reply, "blocks IQ lastTimestamp attribute not valid");
        	return reply;
        }
		
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		Element childElement = XmlParser.elementFromXml(glue.getBlocksXml(from.getNode(), lastTimestamp,
				0 /* start */, 10 /* count */));		

		reply.setChildElement(childElement);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}

}
