package com.dumbhippo.jive;

import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.live.DesktopSettingChangedEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.views.UserViewpoint;

/** 
 * IQ handler for getting your desktop settings
 * 
 * @author Havoc Pennington
 *
 */
@IQHandler(namespace=SettingsIQHandler.SETTINGS_NAMESPACE)
public class SettingsIQHandler extends AnnotatedIQHandler implements LiveEventListener<DesktopSettingChangedEvent> {
	static final String SETTINGS_NAMESPACE = "http://dumbhippo.com/protocol/settings";
	
	@EJB
	private DesktopSettings settings;
	
	@EJB
	private IdentitySpider spider;
	
	public SettingsIQHandler() {
		super("Hippo settings IQ Handler");
		Log.debug("creating SettingsIQHandler");
	}
	
	public void onEvent(DesktopSettingChangedEvent event) {
		User user = spider.lookupUser(event.getUserId()); 

		XmlBuilder xml = new XmlBuilder();
		xml.openElement("settings",
			    "xmlns", SETTINGS_NAMESPACE);
		
		xml.appendTextNode("setting", event.getValue(), "key", event.getKey(),
				"unset", event.getValue() != null ? null : "true");
		
		xml.closeElement();
		
		Message message = new Message();
		message.setType(Message.Type.headline);
		message.getElement().add(XmlParser.elementFromXml(xml.toString()));
		MessageSender.getInstance().sendMessage(user.getGuid(), message);
	}

	@Override
	public void start() throws IllegalStateException {
		super.start();
		Log.debug("setting up DesktopSettingChangedEvent listener");
		LiveState.addEventListener(DesktopSettingChangedEvent.class, this);		
	}

	@Override
	public void stop() {
		super.stop();
		Log.debug("stopping DesktopSettingChangedEvent listener");		
		LiveState.removeEventListener(DesktopSettingChangedEvent.class, this);			
	}	
	
	@IQMethod(name="settings", type=IQ.Type.get)
	public void getSettings(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Map<String,String> map = settings.getSettings(viewpoint.getViewer());
		
		XmlBuilder xml = new XmlBuilder();
		
		xml.openElement("settings",
			    "xmlns", SETTINGS_NAMESPACE);
		
		for (Entry<String,String> entry : map.entrySet()) {
			xml.appendTextNode("setting", entry.getValue(), "key", entry.getKey());
		}
		
		xml.closeElement();
		
		reply.setChildElement(XmlParser.elementFromXml(xml.toString()));
	}
	
	@IQMethod(name="setting", type=IQ.Type.set)
	public void setSetting(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		
		String key = child.attributeValue("key");
		String unset = child.attributeValue("unset");
		String value;
		if (unset != null && unset.equals("true")) {
			value = null;
		} else {
			value = child.getText();
		}
				
		settings.setSetting(viewpoint.getViewer(), key, value);
	}
}
