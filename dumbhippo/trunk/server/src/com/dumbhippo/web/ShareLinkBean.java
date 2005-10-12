package com.dumbhippo.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ShareLinkBean corresponds to the "share a link" JSF page.
 * 
 * @author dff hp
 */

public class ShareLinkBean {
	static Log logger = LogFactory.getLog(ShareLinkBean.class);

	private String url;

	private String description;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		logger.info("Set url = " + url);
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		logger.info("Set description = " + description);
		this.description = description;
	}

	// action handler for form submit
	public String doShareLink() {
		try {
			logger.info("Sharing link!");
			// add the link to the database
			return "main";
		} catch (Exception e) {
			logger.debug(e);
			// didn't work for some reason, just reload the page
			// (should have our JSF message queue displayed in there in theory,
			// with the errors)
			return null;
		}
	}
}