package com.dumbhippo.xmppcom;

import java.util.HashMap;
import java.util.Map;

public class XmppEventMusicChanged extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private Map<String,String> properties;
	private String jabberId;
	
	public XmppEventMusicChanged(String jabberId) {
		this.jabberId = jabberId;
		properties = new HashMap<String,String>();
	}
	
	public String getJabberId() {
		return jabberId;
	}
	
	public Map<String,String> getProperties() {
		return properties;
	}
	
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}
	
	public void addProperties(Map<String,String> properties) {
		this.properties.putAll(properties);
	}
}
