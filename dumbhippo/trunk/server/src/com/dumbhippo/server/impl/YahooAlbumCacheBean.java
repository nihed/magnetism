package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
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
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.persistence.YahooSongResult;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooAlbumCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooAlbumCacheBean extends AbstractCacheBean implements YahooAlbumCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooAlbumCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;		
	
	static private class YahooAlbumSearchTask implements Callable<YahooAlbumResult> {
		
		private YahooSongResult yahooSong;

		public YahooAlbumSearchTask(YahooSongResult yahooSong) {
			this.yahooSong = yahooSong;
		}
		
		public YahooAlbumResult call() {
			logger.debug("Entering YahooAlbumSearchTask thread for yahooSong {}", yahooSong);				

			// we do this instead of an inner class to work right with threads
			YahooAlbumCache cache = EJBUtil.defaultLookup(YahooAlbumCache.class);

			logger.debug("Obtained transaction in YahooAlbumSearchTask thread for yahooSong {}", yahooSong);	
			
			try {
			    YahooAlbumResult yahooAlbumResult = cache.getYahooAlbumSync(yahooSong);
			    return yahooAlbumResult;
			} catch (NotFoundException e) {
				logger.debug(e.getMessage());
				return null;
			}
		}
	}	
	
	static private class YahooAlbumResultsTask implements Callable<List<YahooAlbumResult>> {

		private YahooArtistResult artist;
		private Pageable<AlbumView> albumsByArtist;
		private YahooAlbumResult albumToExclude;
		
		public YahooAlbumResultsTask(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude) {
			this.artist = artist;
			this.albumsByArtist = albumsByArtist;
			this.albumToExclude = albumToExclude;
		}
		
		public List<YahooAlbumResult> call() {
			logger.debug("Entering YahooAlbumTask thread for artist {}", artist);
			// we do this instead of an inner class to work right with threads
			YahooAlbumCache cache = EJBUtil.defaultLookup(YahooAlbumCache.class);
			logger.debug("Obtained transaction in YahooAlbumTask thread for artist {}", artist);			
			return cache.getYahooAlbumResultsSync(artist, albumsByArtist, albumToExclude);
		}
	}

	private List<YahooAlbumResult> updateAlbumResultsSync(List<YahooAlbumResult> oldResults, YahooArtistResult artist, Pageable<AlbumView> albumsByArtist) {
		
		String artistId = artist.getArtistId();

		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		
		int start = 1;		
		int resultsToReturn = albumsByArtist.getInitialPerPage() + albumsByArtist.getSubsequentPerPage();
		boolean getAllResults = false;
		artist.setInitialAlbumsStored(true);
		if (albumsByArtist.getStart() + albumsByArtist.getCount() > resultsToReturn) {
		    resultsToReturn = YahooSearchWebServices.maxResultsToReturn;
		    getAllResults = true;
			artist.setAllAlbumsStored(true);		    
		}
		
		Pair<Integer, List<YahooAlbumResult>> newResultsPair = ws.lookupAlbums(artistId, start, resultsToReturn);
		
		int totalAlbumsAvailable = newResultsPair.getFirst();
		artist.setTotalAlbumsByArtist(totalAlbumsAvailable);
		
		List<YahooAlbumResult> newResults = newResultsPair.getSecond();
		
		start = start + resultsToReturn;
			
		while (getAllResults && (start <= totalAlbumsAvailable)) {
			newResultsPair = ws.lookupAlbums(artistId, start, resultsToReturn);
			start = start + resultsToReturn;
			newResults.addAll(newResultsPair.getSecond());
		}
		
		logger.debug("New album results for artistId {}: {}", artistId, newResults);
		
		// Match new results to old results with same album id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times to include a FAILED_QUERY_TIMEOUT so we don't re-query too often.		
		if (newResults.isEmpty()) {			
			// here we assume that the query failed, so we don't remove any of the old results
			// but what if the new set of results is really empty? for now, this is not a typical 
			// situation when updating artist's music albums			
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - YAHOO_EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			// old results could contain a set of results or a no results marker, in either case
			// we want to slightly update the date on them
			for (YahooAlbumResult old : oldResults) {				
				old.setLastUpdated(new Date(updateTime));
			}
			if (oldResults.isEmpty()) {
				// we got no results, so set the no results marker
				YahooAlbumResult marker = new YahooAlbumResult();
				marker.setArtistId(artistId);
				marker.setNoResultsMarker(true);
				marker.setLastUpdated(new Date(updateTime));
				em.persist(marker);
				// we don't need to return this marker though, returning empty oldResults is good enough				
			}
			
			artist.setLastUpdated(new Date(updateTime));
			
			return oldResults;
		}

		List<YahooAlbumResult> results = new ArrayList<YahooAlbumResult>();
		// for each old result, go through new ones, and see if it is
		// still found among them, if so, update it and remove it from the
		// new results list, so that after this loop, the results that are
		// left in the list are completely new		
		for (YahooAlbumResult old : oldResults) {
			boolean stillFound = false;
			for (YahooAlbumResult n : newResults) {
				if (!old.isNoResultsMarker() &&
					n.getAlbumId().equals(old.getAlbumId())) {
					newResults.remove(n);
					old.update(n); // old is attached, so this gets saved
					results.add(old);
					stillFound = true;
					break;
				}
			}
			// drops the no results marker, and any album no longer found, but only
			// if we were getting all results with this web request (otherwise the
			// list of newResults is not exhaustive)
			if (!stillFound && getAllResults) {
				em.remove(old);
			}
		}
		// remaining newResults weren't previously saved
		for (YahooAlbumResult n : newResults) {
			em.persist(n);
			results.add(n);
		}

		artist.setLastUpdated(new Date());
		
		return results;
	}
	
	// FIXME FIXME FIXME: artist here is modified; but in the case this is called Async, it is attached to the
	// session of the *parent* thread! This basically works because we are modifying only plain data
	// fields, and the changes will get committed when the parent thread's session is flushed to the
	// database, but it's highly improper.
	// (If you change this to pass the artist ID, then you need to change all the callers to make sure
	// that the artist is committed to the database before this function is called)
	//
	// albumToExclude is passed in as committed and detached
	public List<YahooAlbumResult> getYahooAlbumResultsSync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude) {
		
		// we require the artist and the artistId to be set
		if ((artist == null) || (artist.getArtistId() == null)) {
			logger.error("artist or artistId is null when requesting yahoo album results for an artist");
			throw new RuntimeException("artist or artistId is null when requesting yahoo album results for an artist");
		}
		
		String artistId = artist.getArtistId();
				
		boolean needNewQuery = !artist.isInitialAlbumsStored();	
		
		if ((albumsByArtist.getStart() + albumsByArtist.getCount() > 
		     albumsByArtist.getInitialPerPage() + albumsByArtist.getSubsequentPerPage()) &&
			!artist.isAllAlbumsStored()) {
			needNewQuery = true;
		}
		
	    // We do not need to check for album update times in results, because they must be at least
	    // as recent as artist update time if we already stored all albums. We always do the query if
	    // we didn't yet store all albums we'll need.
        long now = System.currentTimeMillis();	
	    if (artist.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT < now) {
		    needNewQuery = true;
		    logger.debug("Outdated Yahoo result, will need to renew the search for artist id {}", artistId);		
	    }
		
		if (needNewQuery) {
	       	// we'll be updating albums in the database, need to pass in everything we have already	
		    Query q = em.createQuery("FROM YahooAlbumResult album WHERE album.artistId = :artistId");
		    q.setParameter("artistId", artistId);

		    List<?> objects = q.getResultList();
		    List<YahooAlbumResult> results = new ArrayList<YahooAlbumResult>(); 
		    for (Object o : objects) {
			    results.add((YahooAlbumResult) o);
		    }
		    
			// passing in the old results here is a race, since the db could have 
			// changed underneath us - but the worst case is that we just fail to
			// get results once in a while
			updateAlbumResultsSync(results, artist, albumsByArtist);
		} 
		

		// now we know our database is in the best standing possible, it's time for THE query
		
		String albumToExcludeClause = "";
		int startResult = albumsByArtist.getStart();
		int maxResults = albumsByArtist.getCount();
		if (albumToExclude != null) {
			albumToExcludeClause = "AND album.albumId != :albumToExcludeId";
			if (albumsByArtist.getStart() == 0) {
				// we know that the excluded album will be at the first position, so we need to return one
				// album less
				maxResults = albumsByArtist.getCount() - 1;
			} else {
				// past the first page, start index that we need to use for the query must be one
				// less that albumsByArtist.getStart(), because the query excludes the first album
				// displayed on the first page
				startResult = albumsByArtist.getStart() - 1;
			}
		}
		
		Query q = em.createQuery("FROM YahooAlbumResult album WHERE album.artistId = :artistId " +
				                 albumToExcludeClause + " ORDER BY album.id");
		
		q.setParameter("artistId", artistId);
		if (albumToExclude != null) {
			q.setParameter("albumToExcludeId", albumToExclude.getAlbumId());
		}
		
		q.setFirstResult(startResult);
		q.setMaxResults(maxResults);
		List<?> objects = q.getResultList();
		List<YahooAlbumResult> results = new ArrayList<YahooAlbumResult>(); 
	    for (Object o : objects) {
		    results.add((YahooAlbumResult) o);
	    }
	    
		logger.debug("Returning Yahoo album results from database cache for artistId {}: {}", artistId, results);
	    return results;

	}
	
	private YahooAlbumResult updateYahooAlbumResultSync(YahooAlbumResult oldResult, YahooSongResult yahooSong) throws NotFoundException {

		String albumId = yahooSong.getAlbumId();
		String artistId = yahooSong.getArtistId();
		
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		try {
		    YahooAlbumResult newResult = ws.lookupAlbum(albumId);
		    logger.debug("New album result for albumId {}: {}", albumId, newResult);
			
		    if (oldResult != null) {
			    // new result always replaces the old result
			    oldResult.update(newResult);
			} else {
				em.persist(newResult);
			}
			
			return newResult;
			
		} catch (NotFoundException e) {
			long updateTime = System.currentTimeMillis() - YAHOO_EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
            if (oldResult != null) {
				oldResult.setLastUpdated(new Date(updateTime));
            } else {
				// we got no results, so set the no results marker
				YahooAlbumResult marker = new YahooAlbumResult();
				marker.setAlbumId(albumId);
				// this is an important bit, because artistId must not be null in the YahooAlbumResult table,
				// and setting it will ensure that if later we query fo albums based on this artistId, and
				// the album with the given albumId becomes available, we will not try to insert an duplicate
				// albumId into the table 
				marker.setArtistId(artistId);
				marker.setNoResultsMarker(true);
				marker.setLastUpdated(new Date(updateTime));
				em.persist(marker);
				throw new NotFoundException("Did not find an album result for albumId " + albumId);
            }
			return oldResult;
		}
	}
	
	public YahooAlbumResult getYahooAlbumSync(YahooSongResult yahooSong) throws NotFoundException {

		String albumId = yahooSong.getAlbumId();
		String artistId = yahooSong.getArtistId();
		
		// all YahooSongResults that are not no results markers must have albumId and artistId, 
		// and this function must not be passed in no results markers
		if ((albumId == null) || (artistId == null)) {
			logger.error("albumId or artistId is null when requesting a single yahoo album result for a yahoo song");
			throw new RuntimeException("albumId or artistId is null when requesting a single yahoo album result for a yahoo song");
		}

		// because we are getting/updating yahoo album result, we do not need to pass in an artistId to the query
		Query q = em.createQuery("FROM YahooAlbumResult album WHERE album.albumId = :albumId");
		q.setParameter("albumId", albumId);

		boolean needNewQuery = false;
		YahooAlbumResult yahooAlbum = null;
        try {
		    yahooAlbum  = (YahooAlbumResult)q.getSingleResult();
		} catch (EntityNotFoundException e) { 
			// we must know about the YahooSongResult because we have a track associated with it, 
			// it's time to get the album info too!
			needNewQuery = true;
		} catch (NonUniqueResultException e) {
			// this should not be allowed by the database schema
			logger.error("Multiple yahoo album results for album id {}", albumId);
			throw new RuntimeException("Multiple yahoo album results for album id " + albumId);
		}
	    
		if (!needNewQuery) {
			// the outdated results, if any, could be a special magic result with the NoResultsMarker 
			// flag set or could be a real result
			long now = System.currentTimeMillis();
            if ((yahooAlbum.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
                needNewQuery = true;
			    logger.debug("Outdated Yahoo album result, will need to renew the search for albumId {}", albumId);
            }
        }
		
		if (needNewQuery) {
			// passing in the old result here is a race, since the db could have 
			// changed underneath us - but the worst case is that we just fail to
			// get results once in a while		
			return updateYahooAlbumResultSync(yahooAlbum, yahooSong);
		} else {
			logger.debug("Returning Yahoo album result from database cache for yahooSong {}: {}", yahooSong, yahooAlbum);
			return yahooAlbum;
		}
	}

	public Future<List<YahooAlbumResult>> getYahooAlbumResultsAsync(YahooArtistResult artist, Pageable<AlbumView> albumsByArtist, YahooAlbumResult albumToExclude) {
		FutureTask<List<YahooAlbumResult>> futureAlbums =
			new FutureTask<List<YahooAlbumResult>>(new YahooAlbumResultsTask(artist, albumsByArtist, albumToExclude));
		getThreadPool().execute(futureAlbums);
		return futureAlbums;
	}

	public Future<YahooAlbumResult> getYahooAlbumAsync(YahooSongResult yahooSong) {
		FutureTask<YahooAlbumResult> futureAlbum =
			new FutureTask<YahooAlbumResult>(new YahooAlbumSearchTask(yahooSong));
		getThreadPool().execute(futureAlbum);
		return futureAlbum;
	}
}
