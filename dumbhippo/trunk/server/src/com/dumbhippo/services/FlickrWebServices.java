package com.dumbhippo.services;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.impl.ConfigurationBean;

public class FlickrWebServices extends AbstractXmlRequest<FlickrSaxHandler> {

	static private final Logger logger = GlobalSetup.getLogger(FlickrWebServices.class);

	private String apiId;
	
	public FlickrWebServices(int timeoutMilliseconds, Configuration config) {
		super(timeoutMilliseconds);
		try {
			this.apiId = config.getPropertyNoDefault(HippoProperty.FLICKR_API_ID);
			if (apiId.trim().length() == 0)
				apiId = null;
		} catch (PropertyNotFoundException e) {
			apiId = null;
		}

		if (apiId == null)
			logger.warn("Flickr app ID is not set, can't make Flickr web services calls.");
	}

	private FlickrSaxHandler doFlickrCall(String method, String... keysAndValues) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://www.flickr.com/services/rest/?method=");
		sb.append(method);
		sb.append("&api_key=");
		sb.append(apiId);
		if ((keysAndValues.length % 2) != 0)
			throw new RuntimeException("key-value pairs array should have even number of elements");
		for (int i = 0; i < keysAndValues.length; i += 2) {
			sb.append("&");
			sb.append(keysAndValues[i]);
			sb.append("=");
			sb.append(StringUtils.urlEncode(keysAndValues[i+1]));
		}

		String wsUrl = sb.toString();
		logger.debug("Running Flickr API call {}", wsUrl);
		
		return parseUrl(new FlickrSaxHandler(), wsUrl);
	}
	
	public String lookupFlickrIdByEmail(String email) {
		FlickrSaxHandler handler = doFlickrCall("flickr.people.findByEmail",
				"find_email", email);
		if (handler == null)
			return null;
		else
			return handler.getNsid();
	}
	
	static public final void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
				
		ConfigurationBean config = new ConfigurationBean();
		config.init();
		
		FlickrWebServices ws = new FlickrWebServices(6000, config);
		String user = ws.lookupFlickrIdByEmail("hp@pobox.com");
		System.out.println("Got flickr user id: '" + user + "'");
	}
}
