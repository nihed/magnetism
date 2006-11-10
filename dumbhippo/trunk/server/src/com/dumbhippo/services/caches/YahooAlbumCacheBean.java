package com.dumbhippo.services.caches;

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
import com.dumbhippo.persistence.CachedYahooAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.services.caches.YahooAlbumCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooAlbumCacheBean extends AbstractCacheBean<String,YahooAlbumData,AbstractCache> implements YahooAlbumCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooAlbumCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;

	public YahooAlbumCacheBean() {
		super(Request.YAHOO_ALBUM, YahooAlbumCache.class, YAHOO_EXPIRATION_TIMEOUT);
	}
	
	static private class YahooAlbumSearchTask implements Callable<YahooAlbumData> {
		
		private String albumId;

		public YahooAlbumSearchTask(String albumId) {
			this.albumId = albumId;
		}
		
		public YahooAlbumData call() {
			logger.debug("Entering YahooAlbumSearchTask thread for albumId {}", albumId);				

			YahooAlbumCache cache = EJBUtil.defaultLookup(YahooAlbumCache.class);	
						
			// Check again in case another node stored the data first
			try {
				return cache.checkCache(albumId);
			} catch (NotCachedException e) {
				YahooAlbumData data = cache.fetchFromNet(albumId);

				return cache.saveInCache(albumId, data);
			}
		}
	}	
	
	public YahooAlbumData getSync(String albumId) {
		return getFutureResultNullOnException(getAsync(albumId));
	}

	public Future<YahooAlbumData> getAsync(String albumId) {
		if (albumId == null) {
			throw new IllegalArgumentException("null albumId passed to YahooAlbumCache");
		}
		
		try {
			YahooAlbumData result = checkCache(albumId);
			return new KnownFuture<YahooAlbumData>(result);
		} catch (NotCachedException e) {
			return getExecutor().execute(albumId, new YahooAlbumSearchTask(albumId));	
		}
	}

	private CachedYahooAlbumData albumResultQuery(String albumId) {
		Query q;
		
		q = em.createQuery("FROM CachedYahooAlbumData album WHERE album.albumId = :albumId");
		q.setParameter("albumId", albumId);
		
		try {
			return (CachedYahooAlbumData) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	public YahooAlbumData checkCache(String albumId) throws NotCachedException {
		CachedYahooAlbumData result = albumResultQuery(albumId);

		if (result != null) {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + YAHOO_EXPIRATION_TIMEOUT) < now)) {
				result = null;
			}
		}

		if (result != null) {
			logger.debug("Have cached yahoo album result for albumId {}: {}",
					albumId, result);
			if (result.getAlbum() == null) // no result marker
				return null;
			else
				return result.toData();
		} else {
			throw new NotCachedException();
		}
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public YahooAlbumData fetchFromNet(String albumId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		YahooAlbumData data = ws.lookupAlbum(albumId);
		logger.debug("Fetched album data for id {}: {}", albumId, data);
		return data;
	}

	// null data means to save a negative result
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public YahooAlbumData saveInCache(final String albumId, final YahooAlbumData data) {
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<YahooAlbumData>() {
				public YahooAlbumData call() {
					CachedYahooAlbumData r = albumResultQuery(albumId);
					if (r == null) {
						// data is allowed to be null which saves the negative result row
						// in the db
						r = new CachedYahooAlbumData();
						r.updateData(albumId, data);
						em.persist(r);
					} else {
						if (data != null) // don't ever save a negative result once we have data at some point
							r.updateData(albumId, data);
					}
					r.setLastUpdated(new Date());
					
					logger.debug("Saved new yahoo album data id {}: {}", 
						     albumId, r);
					
					if (r.isNoResultsMarker())
						return null;
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
