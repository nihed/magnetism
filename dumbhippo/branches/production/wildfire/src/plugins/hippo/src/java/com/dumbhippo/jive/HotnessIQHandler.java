package com.dumbhippo.jive;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.live.Hotness;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

public class HotnessIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public HotnessIQHandler() {
		super("DumbHippo Hotness IQ Handler");
		
		Log.debug("creating Hotness handler");
		info = new IQHandlerInfo("hotness", "http://dumbhippo.com/protocol/hotness");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		Hotness hotness = glue.getUserHotness(from.getNode());
		
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("hotness", "http://dumbhippo.com/protocol/hotness");
		childElement.addAttribute("value", hotness.name());
		reply.setChildElement(childElement);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
