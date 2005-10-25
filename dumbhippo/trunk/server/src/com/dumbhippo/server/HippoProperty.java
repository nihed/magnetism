package com.dumbhippo.server;

public enum HippoProperty {
	BASEURL("dumbhippo.server.baseurl", "http://dumbhippo.com"),
	XMPP_ADDRESS("dumbhippo.server.xmpp.address", null),
	XMPP_PORT("dumbhippo.server.xmpp.port", null),
	XMPP_ADMINUSER("dumbhippo.server.xmpp.adminuser", null),
	XMPP_PASSWORD("dumbhippo.server.xmpp.password", null);
	
	private String key;
	private String def;
	
	HippoProperty(String key, String def) {
		this.key = key;
		this.def = def;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getDefault() {
		return def;
	}
}
