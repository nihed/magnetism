package com.dumbhippo.server.impl;

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
import com.dumbhippo.persistence.CachedAmazonAlbumData;
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
public class AmazonAlbumCacheBean extends AbstractCacheBean<String,AmazonAlbumData> implements AmazonAlbumCache {
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
	
	public AmazonAlbumCacheBean() {
		super(Request.AMAZON_ALBUM);
	}	
	
	static private class AmazonAlbumSearchTask implements Callable<AmazonAlbumData> {
		
		private String album;
		private String artist;

		public AmazonAlbumSearchTask(String album, String artist) {
			this.album = album;
			this.artist = artist;
		}
		
		public AmazonAlbumData call() {
			logger.debug("Entering AmazonAlbumSearchTask thread for album {} by artist {}", album, artist);				

			AmazonAlbumCache cache = EJBUtil.defaultLookup(AmazonAlbumCache.class);	
						
			AmazonAlbumData data = cache.fetchFromNet(album, artist);

			return cache.saveInCache(album, artist, data);
		}
	}

	public Future<AmazonAlbumData> getAsync(String album, String artist) {
		if (artist == null || album == null) {
			logger.debug("missing artist or album, not looking up album {} by artist {} on amazon", 
					     album, artist);
			return new KnownFuture<AmazonAlbumData>(null);
		}
		
		AmazonAlbumData result = checkCache(album, artist);
		if (result != null) {
			if (result.getASIN() == null) // cached negative
				result = null;
			return new KnownFuture<AmazonAlbumData>(result);
		}
		
		// the colon in the middle is to avoid "ab c" and "a bc" resulting in the same key
		return getExecutor().execute(album + ":" + artist, new AmazonAlbumSearchTask(album, artist));		
	}
	
	private CachedAmazonAlbumData albumResultQuery(String album, String artist) {
		Query q;
		
		q = em.createQuery("FROM CachedAmazonAlbumData album WHERE album.artist = :artist AND album.album = :album");
		q.setParameter("artist", artist);
		q.setParameter("album", album);
		
		try {
			return (CachedAmazonAlbumData) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}	
		
	public AmazonAlbumData getSync(final String album, final String artist) {
		return getFutureResultNullOnException(getAsync(album, artist));
	}
	
	public AmazonAlbumData checkCache(String album, String artist) {
		if (artist == null || album == null) {
			throw new IllegalArgumentException("can't check cache for amazon album info with null artist or album");
		}
		
		CachedAmazonAlbumData result = albumResultQuery(album, artist);

		if (result != null) {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + AMAZON_EXPIRATION_TIMEOUT) < now)) {
				result = null;
			}
		}

		if (result != null) {
			logger.debug("Have cached amazon album result for album {} by artist {}: {}", 
					new Object[]{album, artist, result});
			return result.toData();
		} else {
			return null;
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public AmazonAlbumData fetchFromNet(String album, String artist) {
		if (artist == null || album == null) {
			throw new IllegalArgumentException("can't fetch amazon album info from net with null artist or album");
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
		
		return data;
	}
	
	// null data means to save a negative result
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public AmazonAlbumData saveInCache(final String album, final String artist, final AmazonAlbumData data) {
		if (artist == null || album == null) {
			throw new IllegalArgumentException("can't cache amazon album info with null artist or album");
		}
		
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<AmazonAlbumData>() {
				public AmazonAlbumData call() {
					CachedAmazonAlbumData r = albumResultQuery(album, artist);
					if (r == null) {
						// data is allowed to be null which saves the negative result row
						// in the db
						r = new CachedAmazonAlbumData(artist, album, data);
						r.setLastUpdated(new Date());
						em.persist(r);
					} else {
						if (data != null) // don't ever save a negative result once we have data at some point
							r.updateData(data);
						r.setLastUpdated(new Date());
					}
					
					logger.debug("Saved new amazon search result for album {} by artist {}: {}", 
						     new Object[]{album, artist, r});
					
					if (r.getASIN() == null)
						return null; // "no results" marker
					else
						return r.toData();
				}
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception {}: {}", e.getClass().getName(), e.getMessage());
				return data;
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
	}
}
