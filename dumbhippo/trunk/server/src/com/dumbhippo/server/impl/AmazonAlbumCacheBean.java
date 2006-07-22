package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.server.AmazonAlbumCache;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.AmazonItemSearch;

@BanFromWebTier
@Stateless
public class AmazonAlbumCacheBean extends AbstractCacheBean implements AmazonAlbumCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonAlbumCacheBean.class);
	
	// 14 days since we aren't getting price information
	static private final int AMAZON_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;	
	
	@EJB
	private Configuration config;	
	
	static private class AmazonAlbumSearchTask implements Callable<AmazonAlbumResult> {
		
		private String album;
		private String artist;

		public AmazonAlbumSearchTask(String album, String artist) {
			this.album = album;
			this.artist = artist;
		}
		
		public AmazonAlbumResult call() {
			logger.debug("Entering AmazonAlbumSearchTask thread for album {} by artist {}", album, artist);				

			// we do this instead of an inner class to work right with threads
			AmazonAlbumCache cache = EJBUtil.defaultLookup(AmazonAlbumCache.class);

			logger.debug("Obtained transaction in AmazonAlbumSearchTask thread for album {} by artist {}", album, artist);	
			
			return cache.getAmazonAlbumSync(album, artist);
		}
	}

	public Future<AmazonAlbumResult> getAmazonAlbumAsync(String album, String artist) {
		FutureTask<AmazonAlbumResult> futureAlbum =
			new FutureTask<AmazonAlbumResult>(new AmazonAlbumSearchTask(album, artist));
		getThreadPool().execute(futureAlbum);
		return futureAlbum;		
	}
	
	private AmazonAlbumResult albumResultQuery(String artist, String album) {
		Query q;
		
		q = em.createQuery("FROM AmazonAlbumResult album WHERE album.artist = :artist AND album.album = :album");
		q.setParameter("artist", artist);
		q.setParameter("album", album);
		
		try {
			return (AmazonAlbumResult) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		} 
	}	
		
	public AmazonAlbumResult getAmazonAlbumSync(final String album, final String artist) {	
		if (artist == null || album == null) {
			logger.debug("missing artist or album, not looking up album {} by artist {} on amazon", 
					     album, artist);
			return null;
		}
		
		AmazonAlbumResult result = albumResultQuery(artist, album);

		if (result != null) {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + AMAZON_EXPIRATION_TIMEOUT) < now)) {
				result = null;
			}
		}

		if (result != null) {
			logger.debug("Using amazon search result from database for album {} by artist {}: {}", 
					     new Object[]{album, artist, result});
			return result;
		}
		
		AmazonItemSearch search = new AmazonItemSearch(REQUEST_TIMEOUT);
		String amazonKey;
		final AmazonAlbumData data;
		try {
			amazonKey = config.getPropertyNoDefault(HippoProperty.AMAZON_ACCESS_KEY_ID);
		} catch (PropertyNotFoundException e) {
			amazonKey = null;
		}
		if (amazonKey != null)
			data = search.searchAlbum(amazonKey, artist, album);
		else
			data = null;
		
		try {
			AmazonAlbumResult detached = runner.runTaskRetryingOnConstraintViolation(new Callable<AmazonAlbumResult>() {
				
				public AmazonAlbumResult call() {
					AmazonAlbumResult r = albumResultQuery(artist, album);
					if (r == null) {
						// data may be null, in which case the AmazonAlbumResult just 
						// caches that we have no result
						r = new AmazonAlbumResult(artist, album, data);
						r.setLastUpdated(new Date());
						em.persist(r);
					} else {
						if (data != null)
							r.updateData(data);
						else 
							; // if we got no data, just keep whatever the old data was
						r.setLastUpdated(new Date());
					}
					return r;
				}
				
			});
			
			result = detached;
		} catch (Exception e) {
			result = null;
		}
		
		if (result != null)
			logger.debug("Using new amazon search result for album {} by artist {}: {}", 
				     new Object[]{album, artist, result});
		return result;
	}
}
