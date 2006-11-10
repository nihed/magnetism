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
import com.dumbhippo.persistence.CachedYahooSongData;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.services.caches.YahooSongCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;
import com.dumbhippo.services.YahooSongData;

@BanFromWebTier
@Stateless
public class YahooSongCacheBean extends AbstractCacheBean<Track,List<YahooSongData>,AbstractCache> implements YahooSongCache {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSongCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private Configuration config;

	public YahooSongCacheBean() {
		super(Request.YAHOO_SONG, YahooSongCache.class, YAHOO_EXPIRATION_TIMEOUT);
	}
	
	static private class YahooSongTask implements Callable<List<YahooSongData>> {

		private Track track;
		
		public YahooSongTask(Track track) {
			this.track = track;
		}
		
		public List<YahooSongData> call() {			
			logger.debug("Entering YahooSongTask thread for track {}", track);

			// we do this instead of an inner class to work right with threads
			YahooSongCache cache = EJBUtil.defaultLookup(YahooSongCache.class);
			
			// Check again in case another node stored the data first
			try {
				List<YahooSongData> alreadyStored = cache.checkCache(track);
				logger.debug("YahooSongTask found results for artist={}, album={}, name={}, nothing to do",
						new Object[] { track.getArtist(), track.getAlbum(), track.getName() }); 
								
				return alreadyStored;
			} catch (NotCachedException e) {
				List<YahooSongData> result = cache.fetchFromNet(track);
			
				return cache.saveInCache(track, result);
			}
		}
	}
	
	public List<YahooSongData> getSync(Track track) {
		return getFutureResultEmptyListOnException(getAsync(track));
	}

	public Future<List<YahooSongData>> getAsync(Track track) {
		// yahoo lookup requires these fields, so just bail without them
		if (track.getArtist() == null ||
			track.getAlbum() == null ||
			track.getName() == null) {
			logger.debug("Track {} missing artist album or name, can't get yahoo stuff", track);
			List<YahooSongData> empty = Collections.emptyList();
			return new KnownFuture<List<YahooSongData>>(empty);
		}
		
		try {
			List<YahooSongData> result = checkCache(track);
			logger.debug("using cached song listing {} song ids for track {}", result.size(), track);
			return new KnownFuture<List<YahooSongData>>(result);
		} catch (NotCachedException e) {
			return getExecutor().execute(track, new YahooSongTask(track));
		}
	}

	private List<CachedYahooSongData> songDataQuery(Track track) {
		Query q = em.createQuery("SELECT song FROM CachedYahooSongData song WHERE song.searchedName = :name AND song.searchedArtist = :artist AND song.searchedAlbum = :album");
		q.setParameter("name", track.getName());
		q.setParameter("artist", track.getArtist());
		q.setParameter("album", track.getAlbum());
		
		List<CachedYahooSongData> results = TypeUtils.castList(CachedYahooSongData.class, q.getResultList());
		return results;
	}

	// returning empty list means up-to-date cache of no results, while returning 
	// null means no up-to-date cache
	public List<YahooSongData> checkCache(Track track) throws NotCachedException {
		List<CachedYahooSongData> old = songDataQuery(track);

		if (old.isEmpty())
			throw new NotCachedException();
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (CachedYahooSongData d : old) {
			if ((d.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("cached yahoo songs for track are outdated, track = {}", track);
			throw new NotCachedException();
		}
		
		if (haveNoResultsMarker) {
			logger.debug("negative result in cache for yahoo songs for track {}", track);
			return Collections.emptyList();
		}
		
		List<YahooSongData> results = new ArrayList<YahooSongData>();
		for (CachedYahooSongData d : old) {
			results.add(d.toData());
		}
		
		return results;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooSongData> fetchFromNet(Track track) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongData> results = ws.lookupSong(track.getArtist(), track.getAlbum(), track.getName());
		
		logger.debug("New Yahoo song results from web service for track {}: {}", track, results);

		return results;
	}

	private CachedYahooSongData createCachedSong(Track track) {
		CachedYahooSongData d = new CachedYahooSongData();
		d.setSearchedAlbum(track.getAlbum());
		d.setSearchedArtist(track.getArtist());
		d.setSearchedName(track.getName());
		return d;
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooSongData> saveInCache(final Track track, List<YahooSongData> newSongs) {
		// null songs doesn't happen but if it did would be the same as empty list
		if (newSongs == null)
			newSongs = Collections.emptyList();

		final List<YahooSongData> songs = newSongs; 
		
		try {
			// if there were unique constraints involved here we'd need to retry on constraint
			// violations and also put in an em.flush after removing old rows			
			return runner.runTaskInNewTransaction(new Callable<List<YahooSongData>>() {
				public List<YahooSongData> call() {
					// remove all old results
					List<CachedYahooSongData> old = songDataQuery(track);
					for (CachedYahooSongData d : old) {
						em.remove(d);
					}
					
					Date now = new Date();
					
					// save new results
					if (songs.isEmpty()) {
						CachedYahooSongData d = createCachedSong(track);
						d.setLastUpdated(now);
						d.setNoResultsMarker(true);
						em.persist(d);
					} else {
						for (YahooSongData s : songs) {
							CachedYahooSongData d = createCachedSong(track);
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
