package com.dumbhippo.services.caches;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Future;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.URLUtils;
import com.dumbhippo.persistence.CachedRhapsodyDownload;
import com.dumbhippo.server.BanFromWebTier;

@BanFromWebTier
//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class RhapsodyDownloadCacheBean extends AbstractBasicCacheWithStorageBean<String,Boolean,CachedRhapsodyDownload> implements
		RhapsodyDownloadCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(RhapsodyDownloadCacheBean.class);

	// 14 days
	static private final int RHAPLINK_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
	
	public RhapsodyDownloadCacheBean() {
		super(Request.RHAPSODY_DOWNLOAD, RhapsodyDownloadCache.class, RHAPLINK_EXPIRATION_TIMEOUT);
	}
	
	/*
	 * Mangle a string to work in a Rhapsody friendly URL - remove all 
	 * punctuation and whitespace and lowercase it
	 */
	private static String rhapString(String s) {
		// strip all non-word characters, e.g. other than [a-zA-Z0-9]
		// TODO: figure out how to deal with broader character set, if Rhapsody even supports that?
		return s.replaceAll("[^a-zA-Z0-9]","").toLowerCase();
	}

	// nothing database-related about this method, so don't force a transaction
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String buildLink(String album, String artist, int track) throws MalformedURLException {
		if (artist == null || album == null || track < 1) {
			logger.debug("missing artist or album or track, not looking up album {} by artist {} on Rhapsody", 
					     album, artist);
			throw new MalformedURLException("Don't have artist, album, or track"); 
		}
		
		// Try to concoct a Rhapsody friendly URL; see:
		//  http://rws-blog.rhapsody.com/rhapsody_web_services/2006/04/new_album_urls.html		
		return "http://play.rhapsody.com/" + rhapString(artist) + "/" + rhapString(album) + "/track-" + track;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Boolean getSync(String album, String artist, int track) {
		String link;
		try {
			link = buildLink(album, artist, track);
		} catch (MalformedURLException e) {
			return null;
		}
		return getSync(link);
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Future<Boolean> getAsync(String album, String artist, int track) {
		String link;
		try {
			link = buildLink(album, artist, track);
		} catch (MalformedURLException e) {
			return new KnownFuture<Boolean>(null);
		}
		return getAsync(link);		
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public Boolean checkCache(String link) throws NotCachedException {
		
		// allowing a null return here is just annoying for everyone 
		// (null return means "no results marker" which we treat the 
		// same as a result of false)
		
		Boolean b = super.checkCache(link);
		if (b == null)
			return false;
		else
			return b;
	}

	@Override
	protected Boolean fetchFromNetImpl(String link) {
		boolean found = false;
		try {
			URL u = new URL(link);
			URLConnection connection = URLUtils.openConnection(u);
			if (!(connection instanceof HttpURLConnection)) {
				logger.error("Got a weird connection of type {} for URL {}; skipping", connection.getClass().getName(), u);
			} else {
				logger.debug("Making http request for link {}", link);
				
				HttpURLConnection httpConnection = (HttpURLConnection)connection;
				httpConnection.setRequestMethod("HEAD");
				httpConnection.setInstanceFollowRedirects(false);
				httpConnection.setConnectTimeout(REQUEST_TIMEOUT);
				httpConnection.setReadTimeout(REQUEST_TIMEOUT);
				httpConnection.setAllowUserInteraction(false);
				httpConnection.connect();
				
				// if we get a 200 response it's a playable URL
				// if we get anything else, it's not
				
				int httpResponse = httpConnection.getResponseCode();
				httpConnection.disconnect();
				
				// logger.debug("response for rhaplink {} was {}", link, httpResponse);
				
				if (httpResponse == HttpURLConnection.HTTP_OK) {
					logger.debug("got working rhaplink {}", link);
					found = true;
				}
			}
		} catch (MalformedURLException mue) {
			logger.error("malformed URL when trying to fetch rhapsody link: " + mue.getMessage(), mue);
		} catch (IOException ioe) {
			logger.warn("IO exception when trying to fetch rhapsody link: " + ioe.getMessage(), ioe);
		}
		return found;
	}

	@Override
	public CachedRhapsodyDownload queryExisting(String key) {
		try {
			Query q = em.createQuery("SELECT crd FROM CachedRhapsodyDownload crd WHERE crd.url = :url");
			q.setParameter("url", key);
			return (CachedRhapsodyDownload)(q.getSingleResult());
		} catch (NoResultException e) {
			//logger.debug("No cached rhaplink status for {}", rhaplink);
			return null;
		}
	}

	@Override
	public Boolean resultFromEntity(CachedRhapsodyDownload entity) {
		return entity.isActive();
	}

	@Override
	public CachedRhapsodyDownload entityFromResult(String key, Boolean result) {
		CachedRhapsodyDownload entity = new CachedRhapsodyDownload();
		entity.setUrl(key);
		entity.setActive(result);
		return entity;
	}

	@Override
	public void updateEntityFromResult(String key, Boolean result, CachedRhapsodyDownload entity) {
		entity.setUrl(key);
		entity.setActive(result);
	}

	 @Override
	public CachedRhapsodyDownload newNoResultsMarker(String key) {
		return CachedRhapsodyDownload.newNoResultsMarker();
	}
}
