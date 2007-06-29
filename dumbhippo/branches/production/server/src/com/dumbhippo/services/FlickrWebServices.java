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

public class FlickrWebServices extends AbstractXmlRequest<FlickrSaxHandler> {

	static public final int MIN_PER_PAGE = 1;
	static public final int MAX_PER_PAGE = 500;
	
	// I don't think Flickr guarantees a real max here but all the ids
	// I've seen seem to be about 12 chars - so 20 is hopefully generous
	static public final int MAX_FLICKR_USER_ID_LENGTH = 20;
	
	// I don't think Flickr guarantees a real max here either, but 
	// have seen up to about 18
	static public final int MAX_FLICKR_PHOTOSET_ID_LENGTH = 30;
	
	// again, no max defined by Flickr but this is based on practice
	// where 8 chars seems typical
	static public final int MAX_FLICKR_PHOTO_ID_LENGTH = 20;
	
	// ditto, usually about 10 chars
	static public final int MAX_FLICKR_SECRET_LENGTH = 20;
	
	// ditto - this is usually only about 2 chars
	static public final int MAX_FLICKR_SERVER_LENGTH = 10;
	
	static private final Logger logger = GlobalSetup.getLogger(FlickrWebServices.class);

	private String apiId;
	
	static private String loadApiId(Configuration config) {
		String apiId;
		try {
			apiId = config.getPropertyNoDefault(HippoProperty.FLICKR_API_ID);
			if (apiId.trim().length() == 0)
				apiId = null;
		} catch (PropertyNotFoundException e) {
			apiId = null;
		}
		return apiId;
	}
	
	public FlickrWebServices(int timeoutMilliseconds, Configuration config) {
		this(timeoutMilliseconds, loadApiId(config));
	}

	public FlickrWebServices(int timeoutMilliseconds, String apiId) {
		super(timeoutMilliseconds);
		this.apiId = apiId;

		if (this.apiId == null)
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
		
		// too verbose, since we do it in a periodic "cron"
		//logger.debug("Running Flickr API call {}", wsUrl);
		
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
	 * @param perPage how many per page; Flickr ignores it if less than 1 and clamps it to 500
	 * @return photos or null on failure
	 */
	public FlickrPhotos lookupPublicPhotos(String userId, int page, int perPage) {
		if (page < 1)
			throw new RuntimeException("Flickr photo pages count from 1");
		FlickrSaxHandler handler = doFlickrCall("flickr.people.getPublicPhotos",
				"user_id", userId, "page", Integer.toString(page),
				"per_page", Integer.toString(perPage));
		if (handler == null)
			return null;
		else
			return handler.getPhotos();
	}
	
	/**
	 * 
	 * @param photosetId Flickr photoset id
	 * @param page 1-based page to return
	 * @param perPage how many per page; Flickr ignores it if less than 1 and clamps it to 500
	 * @return photos or null on failure
	 */
	public FlickrPhotoset lookupPublicPhotoset(String photosetId, int page, int perPage) {
		if (page < 1)
			throw new RuntimeException("flickr photoset pages count from 1");
		FlickrSaxHandler handler = doFlickrCall("flickr.photosets.getPhotos",
				"photoset_id", photosetId,
				"page", Integer.toString(page),
				"per_page", Integer.toString(perPage));
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
		
		FlickrWebServices ws = new FlickrWebServices(6000, (String)null); // add flickr API id here to test
		FlickrUser user = ws.lookupFlickrUserByEmail("hp@pobox.com");
		System.out.println("Got flickr user: " + user);
		if (user == null)
			return;
		
		FlickrPhotos photos = ws.lookupPublicPhotos(user.getId(), 1, MAX_PER_PAGE);
		System.out.println("Their photos are: " + photos);
		if (photos == null)
			return;
		for (FlickrPhotoView p : photos.getPhotos()) {
			System.out.println("  " + p);
		}
		
		FlickrPhotosets photosets = ws.lookupPublicPhotosets(user.getId());
		System.out.println("Their photosets are:  " + photosets);
		if (photosets == null)
			return;
		
		for (FlickrPhotosetView photoset : photosets.getSets()) {
			System.out.println("Photoset title='" + photoset.getTitle() + "' desc='" + photoset.getDescription() + "'");
			
			FlickrPhotoset filledPhotoset = ws.lookupPublicPhotoset(photoset.getId(), 1, MAX_PER_PAGE);
			System.out.println("Filled photoset is:  " + filledPhotoset);
			if (filledPhotoset == null)
				continue;
			for (FlickrPhotoView p : filledPhotoset.getPhotos().getPhotos()) {
				System.out.println("  " + p);
			}
		}
	}
}
