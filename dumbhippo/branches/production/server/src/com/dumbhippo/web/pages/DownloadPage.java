package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.web.Browser;
import com.dumbhippo.web.BrowserBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * @author otaylor
 *
 * Backing bean for the /download page.
 */
public class DownloadPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DownloadPage.class);
	
	private Configuration configuration;
	
	@Browser
	private BrowserBean browser;
	
	public DownloadPage() {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public String getDownloadUrl() {
		if (browser.isLinuxRequested()) {
			return getDownloadUrlLinux();
		} else {
			return getDownloadUrlWindows();
		}
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	public String getDownloadUrlLinux() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX);
	}
	
	public String getDownloadUrlLinuxTar() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX_TAR);
	}

	public String getDownloadUrlLinuxSrpm() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX_SRPM);
	}
}
