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
import com.dumbhippo.server.AlbumAndArtist;
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
public class AmazonAlbumCacheBean extends AbstractCacheBean<AlbumAndArtist,AmazonAlbumData> implements AmazonAlbumCache {
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
		
		private AlbumAndArtist albumAndArtist;

		public AmazonAlbumSearchTask(AlbumAndArtist albumAndArtist) {
			this.albumAndArtist = albumAndArtist;
		}
		
		public AmazonAlbumData call() {
			logger.debug("Entering AmazonAlbumSearchTask thread for {}", albumAndArtist);
			
			AmazonAlbumCache cache = EJBUtil.defaultLookup(AmazonAlbumCache.class);	
						
			// Check again in case another node stored the data first
			AmazonAlbumData alreadyStored = cache.checkCache(albumAndArtist);
			if (alreadyStored != null)
				return alreadyStored;

			AmazonAlbumData data = cache.fetchFromNet(albumAndArtist);

			return cache.saveInCache(albumAndArtist, data);
		}
	}

	public Future<AmazonAlbumData> getAsync(AlbumAndArtist albumAndArtist) {
		if (albumAndArtist.getArtist() == null || albumAndArtist.getAlbum() == null) {
			logger.debug("missing artist or album, not looking up {} on amazon", 
					     albumAndArtist);
			return new KnownFuture<AmazonAlbumData>(null);
		}
		
		AmazonAlbumData result = checkCache(albumAndArtist);
		if (result != null) {
			if (result.getASIN() == null) // cached negative
				result = null;
			return new KnownFuture<AmazonAlbumData>(result);
		}
		
		return getExecutor().execute(albumAndArtist, new AmazonAlbumSearchTask(albumAndArtist));		
	}
	
	private CachedAmazonAlbumData albumResultQuery(AlbumAndArtist albumAndArtist) {
		Query q;
		
		String album = albumAndArtist.getAlbum();
		String artist = albumAndArtist.getArtist();
		
		q = em.createQuery("FROM CachedAmazonAlbumData album WHERE album.artist = :artist AND album.album = :album");
		q.setParameter("artist", artist.substring(0, Math.min(CachedAmazonAlbumData.DATA_COLUMN_LENGTH, artist.length())));
		q.setParameter("album", album.substring(0, Math.min(CachedAmazonAlbumData.DATA_COLUMN_LENGTH, album.length())));
		
		try {
			return (CachedAmazonAlbumData) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}	
		
	public AmazonAlbumData getSync(AlbumAndArtist albumAndArtist) {
		return getFutureResultNullOnException(getAsync(albumAndArtist));
	}
	
	public AmazonAlbumData checkCache(AlbumAndArtist albumAndArtist) {
		CachedAmazonAlbumData result = albumResultQuery(albumAndArtist);

		if (result != null) {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + AMAZON_EXPIRATION_TIMEOUT) < now)) {
				result = null;
			}
		}

		if (result != null) {
			logger.debug("Have cached amazon album result for {}: {}", 
					albumAndArtist, result);
			return result.toData();
		} else {
			return null;
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public AmazonAlbumData fetchFromNet(AlbumAndArtist albumAndArtist) {

		AmazonItemSearch search = new AmazonItemSearch(REQUEST_TIMEOUT);
		String amazonKey;
		String amazonAssociateTag;
		final AmazonAlbumData data;
		try {
			amazonKey = config.getPropertyNoDefault(HippoProperty.AMAZON_ACCESS_KEY_ID);
		} catch (PropertyNotFoundException e) {
			amazonKey = null;
		}
		
		try {
			amazonAssociateTag = config.getPropertyNoDefault(HippoProperty.AMAZON_ASSOCIATE_TAG_ID);
			if (amazonAssociateTag.trim().length() == 0)
				amazonAssociateTag = null;
		} catch (PropertyNotFoundException e) {
			amazonAssociateTag = null;
		}
		
		if (amazonKey != null)
			// careful, the artist and album are backward from other apis
			data = search.searchAlbum(amazonKey, amazonAssociateTag, albumAndArtist.getArtist(), albumAndArtist.getAlbum());
		else
			data = null;
		
		return data;
	}
	
	// null data means to save a negative result
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public AmazonAlbumData saveInCache(final AlbumAndArtist albumAndArtist, final AmazonAlbumData data) {
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<AmazonAlbumData>() {
				public AmazonAlbumData call() {
					CachedAmazonAlbumData r = albumResultQuery(albumAndArtist);
					if (r == null) {
						// data is allowed to be null which saves the negative result row
						// in the db
						r = new CachedAmazonAlbumData(albumAndArtist, data);
						r.setLastUpdated(new Date());
						em.persist(r);
					} else {
						if (data != null) // don't ever save a negative result once we have data at some point
							r.updateData(data);
						r.setLastUpdated(new Date());
					}
					
					logger.debug("Saved new amazon search result for {}: {}", albumAndArtist, r); 
					
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
