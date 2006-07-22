package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooSongResult;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooSongCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooSongCacheBean extends AbstractCacheBean implements YahooSongCache {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSongCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private Configuration config;		

	/**
	 * A YahooSongTask can be created based on a track for which we want to find matching
	 * song info from yahoo or based on an album for which we want to find all the songs
	 * that are recorded on that album.
	 */
	static private class YahooSongTask implements Callable<List<YahooSongResult>> {

		private Track track;
		private String albumId;
		
		public YahooSongTask(Track track) {
			this.track = track;
		}
		
		public YahooSongTask(String albumId) {
			this.albumId = albumId;
		}
		
		public List<YahooSongResult> call() {
			if (track != null) {			
			    logger.debug("Entering YahooSongTask thread for track {}", track);
			} else {
			    logger.debug("Entering YahooSongTask thread for album id {}", albumId);				
			}
			
			// we do this instead of an inner class to work right with threads
			YahooSongCache cache = EJBUtil.defaultLookup(YahooSongCache.class);

			if (track != null) {			
			    logger.debug("Obtained transaction in YahooSongTask thread for track {}", track);
			} else {
			    logger.debug("Obtained transaction in YahooSongTask thread for album id {}", albumId);				
			}
			
			if (track != null)
				return cache.getYahooSongResultsSync(track);
			
			return cache.getYahooSongResultsSync(albumId);			
		}
	}

	
	private void updateSongResultsSync(List<YahooSongResult> oldResults, Track track) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongResult> newResults = ws.lookupSong(track.getArtist(), track.getAlbum(), track.getName(),
				track.getDuration(), track.getTrackNumber());
		
		logger.debug("New Yahoo song results from web service for track {}: {}", track, newResults);
		logger.debug("Old results: {}", oldResults);		
		// Match new results to old results with same song id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times so we don't re-query too often, if there are no rows add a "no results marker"
		if (newResults.isEmpty()) {
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - YAHOO_EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			for (YahooSongResult old : oldResults) {
				old.setLastUpdated(new Date(updateTime));
			}

			// because this method can be called asynchronously from multiple 
			// places, we should double check if there is already a no results marker 
			// associated with the track, otherwise we get multiple no results markers 
			// associated with a single track, which is harmless, but unnecessary
			// we should only add a no results marker if there are no other results 
			// associated with a track
			if (oldResults.isEmpty() && track.getYahooSongResults().isEmpty()) {
				YahooSongResult marker = new YahooSongResult();
				marker.setNoResultsMarker(true);
				marker.setLastUpdated(new Date(updateTime));
				em.persist(marker);
                logger.debug("adding a no results marker for track {}", track);
                track.addYahooSongResult(marker);
                // we don't need to return this marker though, returning empty oldResults is good enough
			}
			
			return;
		}

		// first go through the old results and see if we just need to update those
		for (YahooSongResult old : oldResults) {
			boolean stillFound = false;
			if (!old.isNoResultsMarker()) {
			    for (YahooSongResult n : newResults) {
				    if (!n.isValid()) {
				    	// ignore the invalid results
				    	continue;
				    }	
				    logger.debug("comparing: {} {}", n.getSongId(), old.getSongId());
				    if (n.getSongId().equals(old.getSongId())) {
					    // it's ok to remove an object from newResults because we break out of the
					    // loop right after it
					    newResults.remove(n);
					    old.update(n); // old is attached, so this gets saved
					    stillFound = true;
				  	    break;
				    }
			    }
			}
			// this should drop the no results marker if we now have results 
			if (!stillFound) {
				track.removeYahooSongResult(old);
				em.remove(old);
			}
		}	
		
		// let's see if any of the new results are already in the YahooSongResult table,
		// if they are, we just need to connect the existing YahooSongResult with this track,
		// they must not have been connected earlier, because the new result was not among the
		// old results associated with the track
		for (Iterator<YahooSongResult> i = newResults.iterator(); i.hasNext();) {
		    YahooSongResult n = i.next();
		    if (!n.isValid()) {
		    	// ignore the invalid results
		    	continue;
		    }		    	
			Query q = em.createQuery("SELECT song FROM YahooSongResult song WHERE song.songId = :songId");
			q.setParameter("songId", n.getSongId());	
			try {
				YahooSongResult song = (YahooSongResult)q.getSingleResult();
				logger.debug("adding song with songId {} to track {}", song.getSongId(), track);
				track.addYahooSongResult(song);
				i.remove();
			} catch (EntityNotFoundException e) {	
				continue;
			} catch (NonUniqueResultException e) {
				logger.warn("non-unique result based on song id {} in YahooSongResult table {}", n.getSongId(), e.getMessage());
				throw e;
			}
		}
		
		// remaining newResults weren't previously saved
		for (YahooSongResult n : newResults) {
		    if (!n.isValid()) {
		    	// ignore the invalid results
		    	continue;
		    }	
			logger.debug("inserting song with songId {} into YahooSongResult table", n.getSongId());
			em.persist(n);
			track.addYahooSongResult(n);
		}
	}
	
	public List<YahooSongResult> getYahooSongResultsSync(Track track) {
		
		// yahoo lookup requires these fields, so just bail without them
		if (track.getArtist() == null ||
				track.getAlbum() == null ||
				track.getName() == null) {
			logger.debug("Track {} missing artist album or name, can't get yahoo stuff", track);
			return Collections.emptyList();
		}
		
		// Track can be detached if we are calling this from YahooSongTask's
		// call() method or from getTrackView() that was called asynchronously, and thus
		// the track we get passed in belongs to a different session.
		if (!em.contains(track)) {
			track = em.find(Track.class, track.getId()); // attach the track to this sesssion
			if (track == null)
				throw new RuntimeException("Transaction isolation mistake (?): can't reattach track");
		} 
		
		List<YahooSongResult> results = new ArrayList<YahooSongResult>();
	    results.addAll(track.getYahooSongResults());
		boolean needNewQuery = results.isEmpty();
		if (!needNewQuery) {
			// the outdated results, if any, could be a special magic result with the NoResultsMarker 
			// flag set, or could be real results
			long now = System.currentTimeMillis();
			for (YahooSongResult r : results) {
				if ((r.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
					needNewQuery = true;
					logger.debug("Outdated Yahoo result, will need to renew the search for track {}", track);
					break;
				}
			}
		}
		if (needNewQuery) {
			updateSongResultsSync(results, track);
			List<YahooSongResult> newResults = new ArrayList<YahooSongResult>();
		    newResults.addAll(track.getYahooSongResults());			
			return newResults;
		} else {
			logger.debug("Returning Yahoo song results from database cache for track {}: {}", track, results);
			return results;
		}
	}

	private List<YahooSongResult> updateSongResultsSync(List<YahooSongResult> oldResults, YahooAlbumResult album) {
		
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		// it can also work based on ws.lookupAlbumSongs(album.getTitle(), album.getArtist());
		List<YahooSongResult> newResults = ws.lookupAlbumSongs(album.getAlbumId());

		logger.debug("New Yahoo song results from web service for album {}: {}", album, newResults);
		logger.debug("Old results: {}", oldResults);		
		// Match new results to old results with same song id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times so we don't re-query too often
		// when querying based on an album, we don't add a "no results marker" if there are no rows
		// returned, the fact that we ran the request is reflected in setting the all songs stored flag 
		// for the album to true
		if (newResults.isEmpty()) {
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - YAHOO_EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			for (YahooSongResult old : oldResults) {
				if (!em.contains(old))
					throw new RuntimeException("old song result should have been attached");
				old.setLastUpdated(new Date(updateTime));
			}
			
			album.setAllSongsStored(true);
			album.setLastUpdated(new Date(updateTime));
			
			return oldResults;
		}
		
		List<YahooSongResult> results = new ArrayList<YahooSongResult>();
		for (YahooSongResult old : oldResults) {
			boolean stillFound = false;
			// noResultsMarker cannot be found among oldResults because
			// we get them by getting songs with a certain (non-null) albumId, 
			// while the noResultsMarker entry never has an albumId set,
			// so we do not need to check for it here
			for (YahooSongResult n : newResults) {
			    if (!n.isValid()) {
			    	// ignore the invalid results
			    	continue;
			    }	
				if (n.getSongId().equals(old.getSongId())) {
					newResults.remove(n);
					old.update(n); // old is attached, so this gets saved
					results.add(old);
					stillFound = true;
					break;
				}
			}
			if (!stillFound) {
				em.remove(old);
			}
		}
		// remaining newResults weren't previously saved
		for (YahooSongResult n : newResults) {
		    if (!n.isValid()) {
		    	// ignore the invalid results
		    	continue;
		    }	
			em.persist(n);
			results.add(n);
		}
		
		// update the last updated time for the album
		// TODO: right now we updated the time even if we only updated the songs, but not the album
		// info, I think this is sufficient, but we might consider rerequesting info for the album too 
		album.setAllSongsStored(true);
		album.setLastUpdated(new Date());
		return results;
	}
	
    public List<YahooSongResult> getYahooSongResultsSync(String albumId) {
		
		// we require the albumId field
		if (albumId == null) {
			logger.error("albumId is null when requesting yahoo songs for an album");
			throw new RuntimeException("albumId is null when requesting yahoo songs for an album");
		}
		
	    Query q = em.createQuery("FROM YahooAlbumResult album WHERE album.albumId = :albumId");	    
		q.setParameter("albumId", albumId);

		YahooAlbumResult yahooAlbum;
		try {
		    yahooAlbum  = (YahooAlbumResult)q.getSingleResult();
		} catch (EntityNotFoundException e) { 
			// we should have stored an album before this call
			logger.error("Did not find a cached yahoo album for album id {}", albumId);
			throw new RuntimeException("Did not find a cached yahoo album for album id " + albumId);
		} catch (NonUniqueResultException e) {
			// this should not be allowed by the database schema
			logger.error("Multiple yahoo album results for album id {}", albumId);
			throw new RuntimeException("Multiple yahoo album results for album id " + albumId);
		}
	    
	    boolean needNewQuery = !yahooAlbum.isAllSongsStored();
	    List<YahooSongResult> results = new ArrayList<YahooSongResult>();
	    
		// even if all songs for the album were not previously stored, we need to supply whichever 
		// songs were stored as oldResults to be updated
		Query queryForAlbumSongs = em.createQuery("SELECT song FROM YahooSongResult song WHERE song.albumId = :albumId");
		queryForAlbumSongs.setParameter("albumId", albumId);

		List<?> songObjects = queryForAlbumSongs.getResultList();
		for (Object o : songObjects) {
			results.add((YahooSongResult) o);
		}
		
		// We do not need to check for song update times in results, because they must be at least
		// as recent as album update time if we already stored all songs. We always do the query if
		// we didn't yet store all songs.
	    long now = System.currentTimeMillis();	
		if (yahooAlbum.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT < now) {
			needNewQuery = true;
			logger.debug("Outdated Yahoo result, will need to renew the search for album id {}", albumId);		
		}

		if (needNewQuery) {
			return updateSongResultsSync(results, yahooAlbum);
		} else {
			logger.debug("Returning Yahoo song results from database cache for album {}: {}", albumId, results);
			return results;
		}
	}	
    
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track) {
		FutureTask<List<YahooSongResult>> futureSong =
			new FutureTask<List<YahooSongResult>>(new YahooSongTask(track));
		getThreadPool().execute(futureSong);
		return futureSong;
	}

	public Future<List<YahooSongResult>> getYahooSongResultsAsync(String albumId) {
		FutureTask<List<YahooSongResult>> futureSong =
			new FutureTask<List<YahooSongResult>>(new YahooSongTask(albumId));
		getThreadPool().execute(futureSong);
		return futureSong;
	}	

}
