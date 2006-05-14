package com.dumbhippo.jive;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;

public class ClientInfoIQHandler extends AbstractIQHandler {
	private IQHandlerInfo info;
	
	public ClientInfoIQHandler() {
		super("Dumbhippo clientInfo IQ Handler");
		Log.debug("creating ClientInfoIQHandler");
		info = new IQHandlerInfo("clientInfo", "http://dumbhippo.com/protocol/clientinfo");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		Log.debug("handling IQ packet " + packet);
		IQ reply = IQ.createResultIQ(packet);
		Element iq = packet.getChildElement();
		
        String platform = iq.attributeValue("platform");
        if (platform == null) {
        	makeError(reply, "clientInfo IQ missing platform attribute");
        	return reply;
        }

		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("clientInfo", "http://dumbhippo.com/protocol/clientinfo");
		if (platform.equals("windows")) {
			childElement.addAttribute("minimum", JiveGlobals.getXMLProperty("dumbhippo.client.windows.minimum"));
			childElement.addAttribute("current", JiveGlobals.getXMLProperty("dumbhippo.client.windows.current"));
			childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.windows.download"));
		} else if (platform.equals("linux")) {
			childElement.addAttribute("minimum", JiveGlobals.getXMLProperty("dumbhippo.client.linux.minimum"));
			childElement.addAttribute("current", JiveGlobals.getXMLProperty("dumbhippo.client.linux.current"));
			childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.linux.download"));
		} else {
			Log.debug("Unknown platform '" + platform + "' in clientInfo IQ");
			makeError(reply, "clientInfo IQ: unrecognized platform: '" + platform + "'");
			return reply;			
		}
		reply.setChildElement(childElement);

     	return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}

	@Override
	public void start() {
	}
}
