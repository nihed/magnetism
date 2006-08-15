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

import com.dumbhippo.ExceptionUtils;
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
		Element prop = parent.addElement("prop");
		prop.addAttribute("key", key);
		prop.setText(value);
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		//Element iq = packet.getChildElement();
		
		Map<String,String> prefs;
		try {
			MessengerGlueRemote glue = EJBUtil.defaultLookupRemote(MessengerGlueRemote.class);
			prefs = glue.getPrefs(from.getNode());
		} catch (Exception e) {
			makeError(reply, "Failed to get prefs from app server: " + ExceptionUtils.getRootCause(e).getMessage());
			return reply;
		}
		
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("prefs", "http://dumbhippo.com/protocol/prefs");
		for (String key : prefs.keySet()) {
			addProp(childElement, key, prefs.get(key));
		}
		reply.setChildElement(childElement);
		
		Log.debug("Sending back prefs " + prefs);
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
}
