package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooSongDownloadCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooSongDownloadCacheBean extends AbstractCacheBean implements YahooSongDownloadCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSongDownloadCacheBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;	
	
	static private class YahooSongDownloadTask implements Callable<List<YahooSongDownloadResult>> {

		private String songId;
		
		public YahooSongDownloadTask(String songId) {
			this.songId = songId;
		}
		
		public List<YahooSongDownloadResult> call() {
			logger.debug("Entering YahooSongDownloadTask thread for songId {}", songId);
			// we do this instead of an inner class to work right with threads
			YahooSongDownloadCache cache = EJBUtil.defaultLookup(YahooSongDownloadCache.class);

		    logger.debug("Obtained transaction in YahooSongDownloadTask thread for songId {}", songId);
		    
			return cache.getYahooSongDownloadResultsSync(songId);
		}
	}

	private List<YahooSongDownloadResult> updateSongDownloadResultsSync(List<YahooSongDownloadResult> oldResults, String songId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongDownloadResult> newResults = ws.lookupDownloads(songId);
		
		logger.debug("New song download results for songId {}: {}", songId, newResults);
		
		// Match new results to old results with same song id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times so we don't re-query too often.
		if (newResults.isEmpty()) {			
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - YAHOO_EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			for (YahooSongDownloadResult old : oldResults) {
				old.setLastUpdated(new Date(updateTime));
			}
			if (oldResults.isEmpty()) {
				YahooSongDownloadResult marker = new YahooSongDownloadResult();
				marker.setSongId(songId);
				marker.setNoResultsMarker(true);
				marker.setLastUpdated(new Date(updateTime));
				em.persist(marker);
				// we don't need to return this marker though, returning empty oldResults is good enough				
			}
			return oldResults;
		}

		List<YahooSongDownloadResult> results = new ArrayList<YahooSongDownloadResult>();
		for (YahooSongDownloadResult old : oldResults) {
			boolean stillFound = false;
			for (YahooSongDownloadResult n : newResults) {
				if (!old.isNoResultsMarker() &&
					n.getSource().equals(old.getSource())) {
					newResults.remove(n);
					old.update(n); // old is attached, so this gets saved
					results.add(old);
					stillFound = true;
					break;
				}
			}
			// drops the no results marker, and any sources no longer found
			if (!stillFound) {
				em.remove(old);
			}
		}
		// remaining newResults weren't previously saved
		// we are only interested in one result per source, so let's keep a list of sources
		// we are adding here; if newResults had a result(s) for a source that already existed in the 
		// database we would have made the appropriate update above
		Set<SongDownloadSource> addedSources = new HashSet<SongDownloadSource>();
		for (YahooSongDownloadResult n : newResults) {
			if (!addedSources.contains(n.getSource())) {
			   em.persist(n);
			   results.add(n);
			   addedSources.add(n.getSource());
			}
		}	
		return results;
	}
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId) {
		
		// we require the songId field
		if (songId == null) {
			logger.error("songId is null when requesting yahoo song downloads for a song");
			throw new RuntimeException("songId is null when requesting yahoo song downloads for a song");
		}
		
		Query q;
		
		q = em.createQuery("FROM YahooSongDownloadResult download WHERE download.songId = :songId");
		q.setParameter("songId", songId);
		
		List<YahooSongDownloadResult> results;
		List<?> objects = q.getResultList();
		results = new ArrayList<YahooSongDownloadResult>(); 
		for (Object o : objects) {
			results.add((YahooSongDownloadResult) o);
		}
		
		boolean needNewQuery = results.isEmpty();
		if (!needNewQuery) {
			// the outdated results, if any, could be a special magic result with the NoResultsMarker 
			// flag set, or could be real results
			long now = System.currentTimeMillis();
			for (YahooSongDownloadResult r : results) {
				if ((r.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
					needNewQuery = true;
					logger.debug("Outdated Yahoo download result, will need to renew the search for songId {}", songId);
					break;
				}
			}
		}
		
		if (needNewQuery) {
			// passing in the old results here is a race, since the db could have 
			// changed underneath us - but the worst case is that we just fail to
			// get results once in a while
			return updateSongDownloadResultsSync(results, songId);
		} else {
			logger.debug("Returning Yahoo song download results from database cache for songId {}: {}", songId, results);
			return results;
		}
	}

	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId) {
		FutureTask<List<YahooSongDownloadResult>> futureDownload =
			new FutureTask<List<YahooSongDownloadResult>>(new YahooSongDownloadTask(songId));
		getThreadPool().execute(futureDownload);
		return futureDownload;		
	}
	

}
