package com.dumbhippo.services.caches;

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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooSongDownload;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.services.caches.YahooSongDownloadCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;
import com.dumbhippo.services.YahooSongDownloadData;

@BanFromWebTier
@Stateless
public class YahooSongDownloadCacheBean extends AbstractCacheBean<String,List<YahooSongDownloadData>> implements YahooSongDownloadCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSongDownloadCacheBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;	

	public YahooSongDownloadCacheBean() {
		super(Request.YAHOO_SONG_DOWNLOAD);
	}
	
	static private class YahooSongDownloadTask implements Callable<List<YahooSongDownloadData>> {

		private String songId;
		
		public YahooSongDownloadTask(String songId) {
			this.songId = songId;
		}
		
		public List<YahooSongDownloadData> call() {			
			logger.debug("Entering YahooSongDownloadTask thread for songId {}", songId);

			// we do this instead of an inner class to work right with threads
			YahooSongDownloadCache cache = EJBUtil.defaultLookup(YahooSongDownloadCache.class);
			
			// Check again in case another node stored the data first
			List<YahooSongDownloadData> alreadyStored = cache.checkCache(songId);
			if (alreadyStored != null)
				return alreadyStored;
			
			List<YahooSongDownloadData> result = cache.fetchFromNet(songId);
			
			return cache.saveInCache(songId, result);
		}
	}
	
	public List<YahooSongDownloadData> getSync(String songId) {
		return getFutureResultEmptyListOnException(getAsync(songId));
	}

	public Future<List<YahooSongDownloadData>> getAsync(String songId) {
		if (songId == null)
			throw new IllegalArgumentException("null songId passed to YahooSongDownloadCacheBean");
		
		List<YahooSongDownloadData> result = checkCache(songId);
		if (result != null)
			return new KnownFuture<List<YahooSongDownloadData>>(result);
		
		return getExecutor().execute(songId, new YahooSongDownloadTask(songId));
	}

	private List<CachedYahooSongDownload> songDataQuery(String songId) {
		Query q = em.createQuery("SELECT song FROM CachedYahooSongDownload song WHERE song.songId = :songId");
		q.setParameter("songId", songId);
		
		List<CachedYahooSongDownload> results = TypeUtils.castList(CachedYahooSongDownload.class, q.getResultList());
		return results;
	}

	// returning empty list means up-to-date cache of no results, while returning 
	// null means no up-to-date cache
	public List<YahooSongDownloadData> checkCache(String songId) {
		List<CachedYahooSongDownload> old = songDataQuery(songId);

		if (old.isEmpty())
			return null;
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (CachedYahooSongDownload d : old) {
			if ((d.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("song download data cache outdated for {}", songId);
			return null;
		}
		
		if (haveNoResultsMarker) {
			logger.debug("negative song download data result cached for {}", songId);
			return Collections.emptyList();
		}
		
		List<YahooSongDownloadData> results = new ArrayList<YahooSongDownloadData>();
		for (CachedYahooSongDownload d : old) {
			results.add(d.toData());
		}
		
		return results;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooSongDownloadData> fetchFromNet(String songId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongDownloadData> results = ws.lookupDownloads(songId);
		
		logger.debug("New Yahoo download results from web service for songId {}: {}", songId, results);

		return results;
	}

	private CachedYahooSongDownload createCachedSong(String songId) {
		CachedYahooSongDownload d = new CachedYahooSongDownload();
		d.setSongId(songId);
		return d;
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooSongDownloadData> saveInCache(final String songId, List<YahooSongDownloadData> newSongs) {
		// null songs doesn't happen but if it did would be the same as empty list
		if (newSongs == null)
			newSongs = Collections.emptyList();

		final List<YahooSongDownloadData> songs = newSongs; 
		
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<List<YahooSongDownloadData>>() {
				public List<YahooSongDownloadData> call() {
					
					// remove all old results
					List<CachedYahooSongDownload> old = songDataQuery(songId);
					
					logger.debug("Saving new song download results in cache; {} old results removed {} to save", old.size(), songs.size());
					
					for (CachedYahooSongDownload d : old) {
						em.remove(d);
					}
					
					// this seems to be required to be sure the rows we just removed don't
					// cause constraint violations; otherwise I guess Hibernate isn't 
					// strict about doing the removes and inserts in order?
					em.flush();
					
					Date now = new Date();
					
					// save new results
					if (songs.isEmpty()) {
						CachedYahooSongDownload d = createCachedSong(songId);
						d.setLastUpdated(now);
						d.setNoResultsMarker(true);
						em.persist(d);
					} else {
						for (YahooSongDownloadData s : songs) {							
							CachedYahooSongDownload d = createCachedSong(songId);
							d.setLastUpdated(now);
							d.updateData(s);
							em.persist(d);
						}
					}
					
					return songs;
				}
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception {}: {}", e.getClass().getName(), e.getMessage());
				return songs;
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
	}
}
