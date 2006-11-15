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

        // optional distribution info
        String distribution = iq.attributeValue("distribution");
        
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("clientInfo", "http://dumbhippo.com/protocol/clientinfo");
		if (platform.equals("windows")) {
			childElement.addAttribute("minimum", JiveGlobals.getXMLProperty("dumbhippo.client.windows.minimum"));
			childElement.addAttribute("current", JiveGlobals.getXMLProperty("dumbhippo.client.windows.current"));
			childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.windows.download"));
		} else if (platform.equals("linux")) {
			childElement.addAttribute("minimum", JiveGlobals.getXMLProperty("dumbhippo.client.linux.minimum"));
			childElement.addAttribute("current", JiveGlobals.getXMLProperty("dumbhippo.client.linux.current"));
			
			// Linux does not use a download url here, because it doesn't auto-download the new package
			// (perhaps it should, but for now it doesn't). We set the url anyway because 1) we might 
			// want to use it later and 2) older clients will break if the attribute is missing.
			if (distribution != null && distribution.equals("fedora5")) {
				childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.fedora5.download"));
			} else if (distribution != null && distribution.equals("fedora6")) {
				childElement.addAttribute("download", JiveGlobals.getXMLProperty("dumbhippo.client.fedora6.download"));
			} else {
				// We set the attribute anyway so older client versions don't throw an error.
				childElement.addAttribute("download", "http://example.com/notused");
			}
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
