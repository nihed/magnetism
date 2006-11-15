package com.dumbhippo.server.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.persistence.CachedRhapsodyDownload;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.RhapsodyDownloadCache;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

@BanFromWebTier
@Stateless
public class RhapsodyDownloadCacheBean extends AbstractCacheBean<String,String> implements
		RhapsodyDownloadCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(RhapsodyDownloadCacheBean.class);

	// 14 days
	static private final int RHAPLINK_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	public RhapsodyDownloadCacheBean() {
		super(Request.RHAPSODY_DOWNLOAD);
	}
	
	static private class RhapsodyLinkTask implements Callable<String> {
		
		private String link;

		public RhapsodyLinkTask(String link) {
			this.link = link;
		}
		
		public String call() {
			logger.debug("In Rhapsody link check thread for {}", link);
			
			RhapsodyDownloadCache cache = EJBUtil.defaultLookup(RhapsodyDownloadCache.class);
			
			// Check again in case another node stored the data first
			Boolean alreadyStored = cache.checkCache(link);
			if (alreadyStored != null)
				return alreadyStored ? link : null;
						
			boolean fetched = cache.fetchFromNet(link);

			return cache.saveInCache(link, fetched);
		}
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

	private CachedRhapsodyDownload rhapLinkQuery(String rhaplink) {
		try {
			Query q = em.createQuery("FROM CachedRhapsodyDownload crd WHERE crd.url = :url");
			q.setParameter("url", rhaplink);
			return (CachedRhapsodyDownload)(q.getSingleResult());
		} catch (NoResultException e) {
			//logger.debug("No cached rhaplink status for {}", rhaplink);
			return null;
		}
	}
	
	public String getSync(String album, String artist, int track) {
		return getFutureResultNullOnException(getAsync(album, artist, track));
	}

	public Future<String> getAsync(String album, String artist, int track) {
		if (artist == null || album == null || track < 1) {
			logger.debug("missing artist or album or track, not looking up album {} by artist {} on Rhapsody", 
					     album, artist);
			return new KnownFuture<String>(null);
		}
		
		// Try to concoct a Rhapsody friendly URL; see:
		//  http://rws-blog.rhapsody.com/rhapsody_web_services/2006/04/new_album_urls.html
		final String link = "http://play.rhapsody.com/" + rhapString(artist) + "/" + rhapString(album) + "/track-" + track;
		
		Boolean result = checkCache(link);
		if (result != null) {
			if (result)
				return new KnownFuture<String>(link);
			else
				return new KnownFuture<String>(null);
		}
		
		return getExecutor().execute(link, new RhapsodyLinkTask(link));
	}

	public Boolean checkCache(String link) {
		CachedRhapsodyDownload oldLink = rhapLinkQuery(link);
		
		final long now = System.currentTimeMillis();
		if ((oldLink == null) ||
			(oldLink.getLastUpdated().getTime() + RHAPLINK_EXPIRATION_TIMEOUT) < now) {
			logger.debug("Unknown or outdated Rhapsody link {}", link);
			return null;
		} else {
			return oldLink.isActive();
		}
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public boolean fetchFromNet(String link) {
		boolean found = false;
		try {
			URL u = new URL(link);
			URLConnection connection = u.openConnection();
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

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public String saveInCache(final String link, final boolean valid) {
		// need to retry on constraint violation to deal with race where multiple TrackViews are
		// being updated in parallel
		try {
			runner.runTaskRetryingOnConstraintViolation(new Runnable() {
				
				public void run() {
					
					logger.debug("saving status = {} for link {}", valid, link);
					
					CachedRhapsodyDownload status = rhapLinkQuery(link);
					if (status == null) {
						status = new CachedRhapsodyDownload();
						status.setUrl(link);
						status.setActive(valid);
						status.setLastUpdated(new Date());
						em.persist(status);
					} else {
						status.setActive(valid);
						status.setLastUpdated(new Date());
					}
				}
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception {}: {}", e.getClass().getName(), e.getMessage());
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
		if (valid)
			return link;
		else
			return null;
	}
}
