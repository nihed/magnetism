package com.dumbhippo.xmppcom;

import java.util.HashMap;
import java.util.Map;

public class XmppEventMusicChanged extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private Map<String,String> properties;

	public XmppEventMusicChanged() {
		properties = new HashMap<String,String>();
	}
	
	public Map<String,String> getProperties() {
		return properties;
	}
	
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}
}
