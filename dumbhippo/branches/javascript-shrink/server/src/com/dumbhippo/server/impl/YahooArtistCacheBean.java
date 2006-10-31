package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooArtistData;
import com.dumbhippo.persistence.CachedYahooArtistIdByName;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooArtistCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooArtistData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooArtistCacheBean extends AbstractCacheBean<String,YahooArtistData> implements YahooArtistCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooArtistCacheBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;		

	public YahooArtistCacheBean() {
		super(Request.YAHOO_ARTIST);
	}
	
	static private class YahooArtistByIdTask implements Callable<YahooArtistData> {
		
		private String artistId;

		public YahooArtistByIdTask(String artistId) {
			this.artistId = artistId;
		}
		
		public YahooArtistData call() {
			logger.debug("Entering YahooArtistByIdTask thread for {}", artistId);				

			YahooArtistCache cache = EJBUtil.defaultLookup(YahooArtistCache.class);	
						
			// Check again in case another node stored the data first
			YahooArtistData alreadyStored = cache.checkCache(artistId);
			if (alreadyStored != null)
				return alreadyStored;
			
			YahooArtistData data = cache.fetchFromNet(artistId);

			return cache.saveInCache(artistId, data);
		}
	}

	static private class YahooArtistByNameTask implements Callable<List<YahooArtistData>> {
		
		private String artist;

		public YahooArtistByNameTask(String artist) {
			this.artist = artist;
		}
		
		public List<YahooArtistData> call() {
			logger.debug("Entering YahooArtistByNameTask thread for '{}'", artist);				

			YahooArtistCache cache = EJBUtil.defaultLookup(YahooArtistCache.class);	
						
			List<YahooArtistData> results = cache.fetchFromNetByName(artist);

			return cache.saveInCacheByName(artist, results);
		}
	}
	
	public YahooArtistData getSync(String artistId) {
		return getFutureResultNullOnException(getAsync(artistId));
	}

	public Future<YahooArtistData> getAsync(String artistId) {
		if (artistId == null)
			throw new IllegalArgumentException("null artistId");
		
		YahooArtistData result = checkCache(artistId);
		if (result != null) {
			if (result.getArtist() == null) // cached negative result
				result = null;
			return new KnownFuture<YahooArtistData>(result);
		}

		return getExecutor().execute(artistId, new YahooArtistByIdTask(artistId));
	}

	public YahooArtistData getSyncByName(String artist) {
		return getFutureResultNullOnException(getAsyncByName(artist));
	}

	static private YahooArtistData pickFirstItemOrNull(List<YahooArtistData> list) {
		if (list == null) {
			return null;
		} else if (list.isEmpty()) {
			return null;
		} else {
			if (list.size() > 1)
				logger.debug("Arbitrarily choosing first artist data of {}", list.size());
			return list.get(0);
		}
	}
	
	static private class PickFirstItemOrNullTask implements Callable<YahooArtistData> {
		
		private Callable<List<YahooArtistData>> listSource;

		public PickFirstItemOrNullTask(Callable<List<YahooArtistData>> listSource) {
			this.listSource = listSource;
		}
		
		public YahooArtistData call() throws Exception {
			return pickFirstItemOrNull(listSource.call());
		}
	}

	public Future<YahooArtistData> getAsyncByName(String artist) {
		if (artist == null)
			throw new IllegalArgumentException("null artist");
		
		List<YahooArtistData> results = checkCacheByName(artist);
		if (results != null)
			return new KnownFuture<YahooArtistData>(pickFirstItemOrNull(results));

		return getExecutor(Request.YAHOO_ARTIST_BY_NAME).execute(artist, new PickFirstItemOrNullTask(new YahooArtistByNameTask(artist)));
	}

	private CachedYahooArtistData artistByIdQuery(String artistId) {
		Query q;
		
		q = em.createQuery("FROM CachedYahooArtistData cyad WHERE cyad.artistId = :artistId");
		q.setParameter("artistId", artistId);
		
		try {
			return (CachedYahooArtistData) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
	private List<CachedYahooArtistIdByName> artistIdsForNameQuery(String artist) {
		Query q;
		
		q = em.createQuery("FROM CachedYahooArtistIdByName byName WHERE byName.name = :name");
		q.setParameter("name", artist);
		
		return TypeUtils.castList(CachedYahooArtistIdByName.class, q.getResultList());
	}
	
	public YahooArtistData checkCache(String artistId) {
		CachedYahooArtistData cached = artistByIdQuery(artistId);
		if (cached == null)
			return null;
		
		long now = System.currentTimeMillis();
		Date lastUpdated = cached.getLastUpdated();
		if (lastUpdated == null || ((lastUpdated.getTime() + YAHOO_EXPIRATION_TIMEOUT) < now)) {
			return null;
		}
		
		return cached.toData();
	}

	public List<YahooArtistData> checkCacheByName(String artist) {
		List<CachedYahooArtistIdByName> ids = artistIdsForNameQuery(artist);
		if (ids.isEmpty())
			return null;
		
		// if any of the cached name-id mappings is null or outdated, we 
		// assume they all are.
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (CachedYahooArtistIdByName id : ids) {
			if (id.isNoResultsMarker())
				haveNoResultsMarker = true;
			if ((id.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now)
				outdated = true;
		}
		
		if (outdated)
			return null;
		
		if (haveNoResultsMarker)
			return Collections.emptyList();

		// if any of the artist id info is outdated / no results, we count the whole 
		// conglomerate of stuff as outdated and redo the yahoo request
		List<YahooArtistData> datas = new ArrayList<YahooArtistData>();
		for (CachedYahooArtistIdByName id : ids) {
			YahooArtistData data = checkCache(id.getArtistId());
			if (data == null) { 
				logger.debug("missing data on id {} so redoing query for name '{}'",
						id.getArtistId(), artist);
				return null;
			}
			datas.add(data);
		}
		
		logger.debug("Using cached data for artist name '{}'", artist);
		return datas;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public YahooArtistData fetchFromNet(String artistId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		YahooArtistData data = ws.lookupArtistById(artistId);
		logger.debug("Fetched artist data for id {}: {}", artistId, data);
		return data;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooArtistData> fetchFromNetByName(String artist) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooArtistData> datas = ws.lookupArtistByName(artist);
		logger.debug("Fetched artist ids/data for name '{}': {}", artist, datas);
		return datas;
	}

	private void saveInCacheInParentTransaction(String artistId, YahooArtistData data, Date now) {
		CachedYahooArtistData d = artistByIdQuery(artistId);
		if (d == null) {
			d = new CachedYahooArtistData();
			// if data is null this just sets all fields to null,
			// which marks negative result
			d.updateData(data);
			em.persist(d);
		} else {
			// never overwrite data if we got it at some point in the past
			if (data != null)
				d.updateData(data);
		}
		d.setLastUpdated(now);
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public YahooArtistData saveInCache(final String artistId, final YahooArtistData data) {
		try {
			runner.runTaskRetryingOnConstraintViolation(new Runnable() {

				public void run() {
					saveInCacheInParentTransaction(artistId, data, new Date());
				}
				
			});
			if (data != null && data.getArtist() == null)
				return null;
			else
				return data;
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

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooArtistData> saveInCacheByName(final String artist, List<YahooArtistData> newArtists) {
		// null doesn't happen but if it did would be the same as empty list
		if (newArtists == null)
			newArtists = Collections.emptyList();

		final List<YahooArtistData> artists = newArtists;
		
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<List<YahooArtistData>>() {
				public List<YahooArtistData> call() {
					// We are saving two different things; the list of id's associated with the 
					// artist name, then the info on each of those ids
					
					Date now = new Date();
					
					// kill old associations with this name
					List<CachedYahooArtistIdByName> ids = artistIdsForNameQuery(artist);
					for (CachedYahooArtistIdByName id : ids) {
						em.remove(id);
					}
					
					// this seems to be required to be sure the rows we just removed don't
					// cause constraint violations; otherwise I guess Hibernate isn't 
					// strict about doing the removes and inserts in order?
					em.flush();					
					
					if (artists.isEmpty()) {
						// save no results marker
						CachedYahooArtistIdByName id = new CachedYahooArtistIdByName();
						id.setLastUpdated(now);
						id.setName(artist);
						id.setArtistId(null); // this is the "no results marker"
						assert id.isNoResultsMarker();
						em.persist(id);	
					} else {
						for (YahooArtistData a : artists) {
							CachedYahooArtistIdByName id = new CachedYahooArtistIdByName();
							id.setLastUpdated(now);
							// this is important; a.getName() is the "canonical" name 
							// while "artist" is the name we searched by. The name we 
							// searched by has to be in the cache or things won't work.
							id.setName(artist);
							id.setArtistId(a.getArtistId());
							em.persist(id);
						}
					}
					
					// Now for each artist id, kill the old data (note that if we have no 
					// results, we just do nothing here - there are no artist ids found 
					// or involved). We would never save negative results here so 
					// we don't need to filter them out.
					for (YahooArtistData a : artists) {
						saveInCacheInParentTransaction(a.getArtistId(), a, now);
					}
					
					return artists;
				}
				
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception {}: {}", e.getClass().getName(), e.getMessage());
				return artists;
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
	}
}
