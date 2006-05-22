package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

/**
 * @author otaylor
 *
 * Backing bean for the /download page.
 */
public class DownloadPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DownloadPage.class);
	
	private Configuration configuration;
	
	public DownloadPage() {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
}
