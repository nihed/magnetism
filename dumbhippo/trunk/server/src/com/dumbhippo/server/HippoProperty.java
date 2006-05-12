package com.dumbhippo.server;

public enum HippoProperty {
	BASEURL("dumbhippo.server.baseurl", "http://dumbhippo.com"),
	DISABLE_AUTHENTICATION("dumbhippo.server.disable.authentication", "false"),
	DISABLE_EMAIL("dumbhippo.server.email.disable", "false"),
	FEEDBACK_EMAIL("dumbhippo.server.email.feedback", "feedback@dumbhippo.com"),
	DOWNLOADURL_WINDOWS("dumbhippo.server.downloadurl.windows", "http://devel.dumbhippo.com/download/DumbHippo-current.msi"),
	XMPP_ADDRESS("dumbhippo.server.xmpp.address", null),
	XMPP_PORT("dumbhippo.server.xmpp.port", null),
	XMPP_ADMINUSER("dumbhippo.server.xmpp.adminuser", null),
	XMPP_PASSWORD("dumbhippo.server.xmpp.password", null),
	FILES_SAVEURL("dumbhippo.server.files.saveurl", null),
	AIMBOT_NAME("dumbhippo.server.aimbot.name", null),
	AMAZON_ACCESS_KEY_ID("dumbhippo.server.amazon.accesskeyid", null),
	EBAY_DEVID("dumbhippo.server.ebay.devid", null),
	EBAY_APPID("dumbhippo.server.ebay.appid", null),
	EBAY_CERTID("dumbhippo.server.ebay.certid", null),
	BUILDSTAMP("dumbhippo.server.buildstamp", "0"), 
	ENABLE_ADMIN_CONSOLE("dumbhippo.server.adminconsole.enable", "false"),
	STEALTH_MODE("dumbhippo.server.stealthmode", "false");
	
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
