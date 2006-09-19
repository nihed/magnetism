package com.dumbhippo.jive;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQHandlerInfo;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.UserPrefChangedEvent;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.util.EJBUtil;

public class PrefsIQHandler extends AbstractIQHandler implements LiveEventListener<UserPrefChangedEvent> {

	private IQHandlerInfo info;
	
	public PrefsIQHandler() {
		super("DumbHippo Prefs IQ Handler");
		
		Log.debug("creating Prefs handler");
		info = new IQHandlerInfo("prefs", "http://dumbhippo.com/protocol/prefs");
	}
	
	private static void addProp(Element parent, String key, String value) {
		Element prop = parent.addElement("prop");
		prop.addAttribute("key", key);
		prop.setText(value);
	}
	
	private static Document prefsToXml(Map<String,String> prefs) {
		Document document = DocumentFactory.getInstance().createDocument();
		Element childElement = document.addElement("prefs", "http://dumbhippo.com/protocol/prefs");
		for (String key : prefs.keySet()) {
			addProp(childElement, key, prefs.get(key));
		}
		return document;
	}
	
	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		
		Log.debug("handling IQ packet " + packet);
		JID from = packet.getFrom();
		IQ reply = IQ.createResultIQ(packet);
		//Element iq = packet.getChildElement();
		
		Map<String,String> prefs;
		try {
			MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
			prefs = glue.getPrefs(from.getNode());
		} catch (Exception e) {
			makeError(reply, "Failed to get prefs from app server: " + ExceptionUtils.getRootCause(e).getMessage());
			return reply;
		}
		
		reply.setChildElement(prefsToXml(prefs).getRootElement());
		
		Log.debug("Sending back prefs " + prefs);
		return reply;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return info;
	}
	
	public void onEvent(UserPrefChangedEvent event) {
		IdentitySpider spider = EJBUtil.defaultLookup(IdentitySpider.class);
		AccountSystem accounts = EJBUtil.defaultLookup(AccountSystem.class);
		
		User user = spider.lookupUser(event.getUserId()); 

		Map<String,String> prefs = accounts.getPrefs(user.getAccount());
		Message message = new Message();
		message.setType(Message.Type.headline);
		message.getElement().add(prefsToXml(prefs).getRootElement());
		MessageSender.getInstance().sendMessage(user.getGuid(), message);
	}

	@Override
	public void start() throws IllegalStateException {
		super.start();
		Log.debug("setting up UserPrefChangedEvent listener");
		LiveState.addEventListener(UserPrefChangedEvent.class, this);		
	}

	@Override
	public void stop() {
		super.stop();
		Log.debug("stopping UserPrefChangedEvent listener");		
		LiveState.removeEventListener(UserPrefChangedEvent.class, this);			
	}	
}
