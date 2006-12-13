package com.dumbhippo.hungry.util;

public enum UserAgent {
	IE6("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.0.3705)"),
	IE6_MCE("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.0.3705; .NET CLR 1.1.4322; Media Center PC 3.1"),
	FIREFOX15_WINDOWS("FIXME"),
	FIREFOX15_LINUX("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8) Gecko/20051103 Fedora/1.5-0.5.0.rc1 Firefox/1.5"),
	SAFARI("Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/416.12 (KHTML, like Gecko) Safari/416.13");
	
	private String text;
	
	private UserAgent(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
}
