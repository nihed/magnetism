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
import com.dumbhippo.persistence.CachedYahooAlbumSongData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooAlbumSongsCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;
import com.dumbhippo.services.YahooSongData;

@BanFromWebTier
@Stateless
public class YahooAlbumSongsCacheBean extends AbstractCacheBean implements YahooAlbumSongsCache {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooAlbumSongsCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private Configuration config;		

	static private class YahooAlbumSongsTask implements Callable<List<YahooSongData>> {

		private String albumId;
		
		public YahooAlbumSongsTask(String albumId) {
			this.albumId = albumId;
		}
		
		public List<YahooSongData> call() {			
			logger.debug("Entering YahooSongTask thread for albumId {}", albumId);

			// we do this instead of an inner class to work right with threads
			YahooAlbumSongsCache cache = EJBUtil.defaultLookup(YahooAlbumSongsCache.class);
			
			List<YahooSongData> result = cache.fetchFromNet(albumId);
			
			return cache.saveInCache(albumId, result);
		}
	}
	
	public List<YahooSongData> getSync(String albumId) {
		return getFutureResult(getAsync(albumId));
	}

	public Future<List<YahooSongData>> getAsync(String albumId) {
		if (albumId == null)
			throw new IllegalArgumentException("null albumId passed to YahooAlbumSongsCacheBean");
		
		List<YahooSongData> result = checkCache(albumId);
		if (result != null)
			return new KnownFuture<List<YahooSongData>>(result);
		
		FutureTask<List<YahooSongData>> futureResult =
			new FutureTask<List<YahooSongData>>(new YahooAlbumSongsTask(albumId));
		getThreadPool().execute(futureResult);
		return futureResult;
	}

	private List<CachedYahooAlbumSongData> songDataQuery(String albumId) {
		Query q = em.createQuery("SELECT song FROM CachedYahooAlbumSongData song WHERE song.albumId = :albumId");
		q.setParameter("albumId", albumId);
		
		List<CachedYahooAlbumSongData> results = TypeUtils.castList(CachedYahooAlbumSongData.class, q.getResultList());
		return results;
	}

	// returning empty list means up-to-date cache of no results, while returning 
	// null means no up-to-date cache
	public List<YahooSongData> checkCache(String albumId) {
		List<CachedYahooAlbumSongData> old = songDataQuery(albumId);

		if (old.isEmpty())
			return null;
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (CachedYahooAlbumSongData d : old) {
			if ((d.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("Cache appears outdated for songs for album {}", albumId);
			return null;
		}
		
		if (haveNoResultsMarker) {
			logger.debug("Negative result cached for songs for album {}", albumId);
			return Collections.emptyList();
		}
		
		List<YahooSongData> results = new ArrayList<YahooSongData>();
		for (CachedYahooAlbumSongData d : old) {
			results.add(d.toData());
		}
		
		return results;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooSongData> fetchFromNet(String albumId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongData> results = ws.lookupAlbumSongs(albumId);
		
		logger.debug("New Yahoo song results from web service for albumId {}: {}", albumId, results);

		return results;
	}

	private CachedYahooAlbumSongData createCachedSong(String albumId) {
		CachedYahooAlbumSongData d = new CachedYahooAlbumSongData();
		d.setAlbumId(albumId);
		return d;
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooSongData> saveInCache(final String albumId, List<YahooSongData> newSongs) {
		// null songs doesn't happen but if it did would be the same as empty list
		if (newSongs == null)
			newSongs = Collections.emptyList();

		final List<YahooSongData> songs = newSongs; 
		
		try {
			return runner.runTaskInNewTransaction(new Callable<List<YahooSongData>>() {
				public List<YahooSongData> call() {
					
					logger.debug("Saving new album song results in cache");
					
					// remove all old results
					List<CachedYahooAlbumSongData> old = songDataQuery(albumId);
					for (CachedYahooAlbumSongData d : old) {
						em.remove(d);
					}
					
					Date now = new Date();
					
					// save new results
					if (songs.isEmpty()) {
						CachedYahooAlbumSongData d = createCachedSong(albumId);
						d.setLastUpdated(now);
						d.setNoResultsMarker(true);
						em.persist(d);
					} else {
						for (YahooSongData s : songs) {
							CachedYahooAlbumSongData d = createCachedSong(albumId);
							d.setLastUpdated(now);
							d.updateData(s);
							em.persist(d);
						}
					}
					
					return songs;
				}
				
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			throw new RuntimeException(e); // not reached			
		}
	}
}
