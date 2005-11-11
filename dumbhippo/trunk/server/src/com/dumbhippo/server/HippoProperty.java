package com.dumbhippo.server;

public enum HippoProperty {
	BASEURL("dumbhippo.server.baseurl", "http://dumbhippo.com"),
	DOWNLOADURL_WINDOWS("dumbhippo.server.downloadurl.windows", "http://devel.dumbhippo.com/download/DumbHippo-current.msi"),
	XMPP_ADDRESS("dumbhippo.server.xmpp.address", null),
	XMPP_PORT("dumbhippo.server.xmpp.port", null),
	XMPP_ADMINUSER("dumbhippo.server.xmpp.adminuser", null),
	XMPP_PASSWORD("dumbhippo.server.xmpp.password", null),
	FILES_SAVEURL("dumbhippo.server.files.saveurl", null),
	AIMBOT_NAME("dumbhippo.server.aimbot.name", null);
	
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
