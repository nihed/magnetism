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
	
	public FlickrUser lookupFlickrUserByEmail(String email) {
		FlickrSaxHandler handler = doFlickrCall("flickr.people.findByEmail",
				"find_email", email);
		if (handler == null)
			return null;
		else
			return handler.getUser();
	}
	
	/**
	 * 
	 * @param userId Flickr NSID
	 * @param page 1-based page to return
	 * @return photos or null on failure
	 */
	public FlickrPhotos lookupPublicPhotos(String userId, int page) {
		if (page < 1)
			throw new RuntimeException("Flickr photo pages count from 1");
		FlickrSaxHandler handler = doFlickrCall("flickr.people.getPublicPhotos",
				"user_id", userId, "page", Integer.toString(page));
		if (handler == null)
			return null;
		else
			return handler.getPhotos();
	}
	
	public FlickrPhotoset lookupPublicPhotoset(String photosetId, int page) {
		if (page < 1)
			throw new RuntimeException("flickr photoset pages count from 1");
		FlickrSaxHandler handler = doFlickrCall("flickr.photosets.getPhotos",
				"photoset_id", photosetId,
				"page", Integer.toString(page));
		if (handler == null)
			return null;
		else
			return handler.getPhotoset();
	}
	
	public FlickrPhotosets lookupPublicPhotosets(String userId) {
		FlickrSaxHandler handler = doFlickrCall("flickr.photosets.getList",
				"user_id", userId);
		if (handler == null)
			return null;
		else
			return handler.getPhotosets();
	}
	
	static public final void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
		
		ConfigurationBean config = new ConfigurationBean();
		config.init();
		
		FlickrWebServices ws = new FlickrWebServices(6000, config);
		FlickrUser user = ws.lookupFlickrUserByEmail("hp@pobox.com");
		System.out.println("Got flickr user: " + user);
		if (user == null)
			return;
		
		FlickrPhotos photos = ws.lookupPublicPhotos(user.getId(), 1);
		System.out.println("Their photos are: " + photos);
		if (photos == null)
			return;
		for (FlickrPhoto p : photos.getPhotos()) {
			System.out.println("  " + p);
		}
		
		FlickrPhotosets photosets = ws.lookupPublicPhotosets(user.getId());
		System.out.println("Their photosets are:  " + photosets);
		if (photosets == null)
			return;
		
		for (FlickrPhotoset photoset : photosets.getSets()) {
			FlickrPhotoset filledPhotoset = ws.lookupPublicPhotoset(photoset.getId(), 1);
			System.out.println("Filled photoset is:  " + filledPhotoset);
			if (filledPhotoset == null)
				continue;
			for (FlickrPhoto p : filledPhotoset.getPhotos().getPhotos()) {
				System.out.println("  " + p);
			}
		}
	}
}
