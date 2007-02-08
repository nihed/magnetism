package com.dumbhippo.jive;

import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.DesktopSettings;
import com.dumbhippo.server.views.UserViewpoint;

/** 
 * IQ handler for getting your desktop settings
 * 
 * @author Havoc Pennington
 *
 */
@IQHandler(namespace=SettingsIQHandler.SETTINGS_NAMESPACE)
public class SettingsIQHandler extends AnnotatedIQHandler {
	static final String SETTINGS_NAMESPACE = "http://dumbhippo.com/protocol/settings";
	
	@EJB
	private DesktopSettings settings;
	
	public SettingsIQHandler() {
		super("Hippo settings IQ Handler");
		Log.debug("creating SettingsIQHandler");
	}
	
	@IQMethod(name="settings", type=IQ.Type.get)
	public void getContacts(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
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
		String value = child.getText();
		
		settings.setSetting(viewpoint.getViewer(), key, value);
	}
}
