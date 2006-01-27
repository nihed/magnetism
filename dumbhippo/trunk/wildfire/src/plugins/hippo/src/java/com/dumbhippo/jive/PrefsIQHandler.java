package com.dumbhippo.jive;

import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

public class PrefsIQHandler extends AbstractIQHandler {

	private IQHandlerInfo info;
	
	public PrefsIQHandler() {
		super("DumbHippo Prefs IQ Handler");
		
		Log.debug("creating Prefs handler");
		info = new IQHandlerInfo("prefs", "http://dumbhippo.com/protocol/prefs");
	}
	
	private void addProp(Element parent, String key, String value) {
		Element prop = parent.getDocument().addElement("prop");
		prop.addAttribute("key", key);
		prop.setText(value);
		parent.add(prop);
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		//Element iq = packet.getChildElement();
		
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		Map<String,String> prefs = glue.getPrefs(from.getNode());
		
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("prefs", "http://dumbhippo.com/protocol/prefs");
		for (String key : prefs.keySet()) {
			addProp(childElement, key, prefs.get(key));
		}
		reply.setChildElement(childElement);
		
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
