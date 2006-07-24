package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooArtistAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooArtistAlbumsCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooArtistAlbumsCacheBean extends AbstractCacheBean implements YahooArtistAlbumsCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooArtistAlbumsCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;
	

	static private class YahooArtistAlbumsTask implements Callable<List<YahooAlbumData>> {

		private String artistId;
		
		public YahooArtistAlbumsTask(String artistId) {
			this.artistId = artistId;
		}
		
		public List<YahooAlbumData> call() {			
			logger.debug("Entering YahooArtistAlbumsTask thread for artistId {}", artistId);

			// we do this instead of an inner class to work right with threads
			YahooArtistAlbumsCache cache = EJBUtil.defaultLookup(YahooArtistAlbumsCache.class);
			
			List<YahooAlbumData> result = cache.fetchFromNet(artistId);
			
			return cache.saveInCache(artistId, result);
		}
	}
	
	public List<YahooAlbumData> getSync(String artistId) {
		return getFutureResult(getAsync(artistId));
	}

	public Future<List<YahooAlbumData>> getAsync(String artistId) {
		if (artistId == null)
			throw new IllegalArgumentException("null artistId passed to YahooArtistAlbumsCacheBean");
		
		List<YahooAlbumData> result = checkCache(artistId);
		if (result != null)
			return new KnownFuture<List<YahooAlbumData>>(result);
		
		FutureTask<List<YahooAlbumData>> futureResult =
			new FutureTask<List<YahooAlbumData>>(new YahooArtistAlbumsTask(artistId));
		getThreadPool().execute(futureResult);
		return futureResult;
	}

	private List<CachedYahooArtistAlbumData> albumDataQuery(String artistId) {
		Query q = em.createQuery("SELECT album FROM CachedYahooArtistAlbumData album WHERE album.artistId = :artistId");
		q.setParameter("artistId", artistId);
		
		List<CachedYahooArtistAlbumData> results = TypeUtils.castList(CachedYahooArtistAlbumData.class, q.getResultList());
		return results;
	}

	// returning empty list means up-to-date cache of no results, while returning 
	// null means no up-to-date cache
	public List<YahooAlbumData> checkCache(String artistId) {
		List<CachedYahooArtistAlbumData> old = albumDataQuery(artistId);

		if (old.isEmpty())
			return null;
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (CachedYahooArtistAlbumData d : old) {
			if ((d.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("Cache appears outdated for artist's album listing {}", artistId);
			return null;
		}
		
		if (haveNoResultsMarker) {
			logger.debug("Negative result cached for artist's album listing {}", artistId);
			return Collections.emptyList();
		}
		
		List<YahooAlbumData> results = new ArrayList<YahooAlbumData>();
		for (CachedYahooArtistAlbumData d : old) {
			results.add(d.toData());
		}
		
		return results;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooAlbumData> fetchFromNet(String artistId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooAlbumData> results = ws.lookupAlbumsByArtistId(artistId);
		
		logger.debug("New Yahoo album results from web service for artistId {}: {}", artistId, results);

		return results;
	}

	private CachedYahooArtistAlbumData createCachedAlbum(String artistId) {
		CachedYahooArtistAlbumData d = new CachedYahooArtistAlbumData();
		d.setAlbumId(artistId);
		return d;
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooAlbumData> saveInCache(final String artistId, List<YahooAlbumData> newAlbums) {
		// null albums doesn't happen but if it did would be the same as empty list
		if (newAlbums == null)
			newAlbums = Collections.emptyList();

		final List<YahooAlbumData> albums = newAlbums; 
		
		try {
			return runner.runTaskInNewTransaction(new Callable<List<YahooAlbumData>>() {
				public List<YahooAlbumData> call() {
					
					logger.debug("Saving new album results in cache for artistId {}", artistId);
					
					// remove all old results
					List<CachedYahooArtistAlbumData> old = albumDataQuery(artistId);
					for (CachedYahooArtistAlbumData d : old) {
						em.remove(d);
					}
					
					Date now = new Date();
					
					// save new results
					if (albums.isEmpty()) {
						CachedYahooArtistAlbumData d = createCachedAlbum(artistId);
						assert d.isNoResultsMarker();
						d.setLastUpdated(now);
						em.persist(d);
					} else {
						for (YahooAlbumData s : albums) {
							CachedYahooArtistAlbumData d = createCachedAlbum(artistId);
							d.setLastUpdated(now);
							d.updateData(s);
							em.persist(d);
						}
					}
					
					return albums;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			throw new RuntimeException(e); // not reached			
		}
	}
}
