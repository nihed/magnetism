package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooArtistCache;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooArtistCacheBean extends AbstractCacheBean implements YahooArtistCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooArtistCacheBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;		

	private String updateYahooArtistResultsSync(List<YahooArtistResult> oldResults, String artist, String artistId) {
		// return an updatedArtistId only if tha passed in artist name will still not be found in the database, 
		// but there is a different name for the same artist in the database
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooArtistResult> newResults = ws.lookupArtist(artist, artistId);
		
		logger.debug("New artist results for artist {} artistId {}: {}", 
				     new Object[]{artist, artistId, newResults});
		
		// Match new results to old results with same artist id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times to include a FAILED_QUERY_TIMEOUT so we don't re-query too often.		
		if (newResults.isEmpty()) {			
			// here we assume that the query failed, so we don't remove any of the old results
			// but what if the new set of results is really empty? for now, this is not a typical 
			// situation when updating an artist	
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - YAHOO_EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			// old results could contain a set of results or a no results marker, in either case
			// we want to slightly update the date on them
			for (YahooArtistResult old : oldResults) {				
				old.setLastUpdated(new Date(updateTime));
			}
			if (oldResults.isEmpty()) {
				// we got no results, so set the no results marker
				YahooArtistResult marker = new YahooArtistResult();
				marker.setArtist(artist);
				marker.setArtistId(artistId);
				marker.setNoResultsMarker(true);
				marker.setLastUpdated(new Date(updateTime));
				em.persist(marker);
				// we don't need to return this marker though, returning empty oldResults is good enough				
			}
			
			return null;
		}

		// for each old result, go through new ones, and see if it is
		// still found among them, if so, update it and remove it from the
		// new results list, so that after this loop, the results that are
		// left in the list are completely new		
		for (YahooArtistResult old : oldResults) {
			boolean stillFound = false;
			for (YahooArtistResult n : newResults) {
				if (!old.isNoResultsMarker() &&
					n.getArtistId().equals(old.getArtistId())) {
					newResults.remove(n);
					old.update(n); // old is attached, so this gets saved
					stillFound = true;
					break;
				}
			}
			// drops the no results marker and any artist no longer found
			// because when we already have multiple artist results in the database for an artist name,
			// we would call update function individually for each artist id in these results, this
			// will not remove any results that are still valid
			if (!stillFound) {
				em.remove(old);
			}
		}
		// remaining newResults weren't previously saved
		for (YahooArtistResult n : newResults) {
			// sometimes different names for the artist map to the same id, i.e. "Belle & Sebastian"  and
			// "Belle and Sebastian" both map to the same id, so if we were looking up an artist result just 
			// based on an artist name, we need to double-check that the artist with this id doesn't 
			// exist in our database
			if (artistId == null) {
			    Query q = em.createQuery("FROM YahooArtistResult artist WHERE artist.artistId = :artistId");
			    q.setParameter("artistId", n.getArtistId());		
		        try {
				    YahooArtistResult yahooArtist  = (YahooArtistResult)q.getSingleResult();
				    logger.debug("Artist with artist id {} already existed in the database: {}", 
				    		     n.getArtistId(), yahooArtist);
				    return n.getArtistId();
				} catch (EntityNotFoundException e) { 
                    // coast is clear, we can insert the new result					
					em.persist(n);
				} catch (NonUniqueResultException e) {
					// this should not be allowed by the database schema
					logger.error("Multiple yahoo artist results for artist id {}", n.getArtistId());
					throw new RuntimeException("Multiple yahoo artist results for artist id " + n.getArtistId());
				}
			} else {
				em.persist(n);		
			}
			
		}
		
		return null;
	}
	
	public YahooArtistResult getYahooArtistResultSync(String artist, String artistId) throws NotFoundException {	
		
		// we require either artist or artistId field
		if ((artist == null) && (artistId == null)) {
			logger.error("Both artist and artistId are null when requesting yahoo artist");
			throw new RuntimeException("Artist is null when requesting yahoo artist");
		}

		Query q;
		if (artistId != null) {
		    q = em.createQuery("FROM YahooArtistResult artist WHERE artist.artistId = :artistId");
		    q.setParameter("artistId", artistId);
		} else {
			// ordering here is important, because it ensures that we always use the same artist id
			// for a given artist name; otherwise we end up with different results when running the same
	    	// searches
		    q = em.createQuery("FROM YahooArtistResult artist WHERE artist.artist = :artist ORDER BY artist.id");
		    q.setParameter("artist", artist);			
		}
		
		List<?> objects = q.getResultList();
		List<YahooArtistResult> results = new ArrayList<YahooArtistResult>(); 
		for (Object o : objects) {
			assert o != null;
			results.add((YahooArtistResult) o);
		}
		
		boolean performedUpdate = false;
		String updatedArtistId = null;
		if (!results.isEmpty()) {
			// the outdated results, if any, could be a special magic result with the NoResultsMarker 
			// flag set, or could be real results
			long now = System.currentTimeMillis();
			for (YahooArtistResult r : results) {
				if ((r.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
					logger.debug("Outdated Yahoo artist result, will need to renew the search for artist {}, artistId {}", artist, artistId);
					updatedArtistId = updateYahooArtistResultsSync(results, r.getArtist(), r.getArtistId());
					performedUpdate = true;
				}
			}
		} else {
			// passing in the old results here is a race, since the db could have 
			// changed underneath us - but the worst case is that we just fail to
			// get results once in a while
			updatedArtistId = updateYahooArtistResultsSync(results, artist, artistId);
			performedUpdate = true;
		}

        if (performedUpdate) {
		    Query newQuery;
		    if (updatedArtistId != null) {
		    	newQuery = em.createQuery("FROM YahooArtistResult artist WHERE artist.artistId = :artistId");
		    	newQuery.setParameter("artistId", updatedArtistId);			
		    } else if (artistId != null) {
		    	newQuery = em.createQuery("FROM YahooArtistResult artist WHERE artist.artistId = :artistId");
		    	newQuery.setParameter("artistId", artistId);
		     } else {
				// ordering here is important, because it ensures that we always use the same artist id
				// for a given artist name; otherwise we end up with different results when running the same
		    	// searches
		    	newQuery = em.createQuery("FROM YahooArtistResult artist WHERE artist.artist = :artist ORDER BY artist.id");
		    	newQuery.setParameter("artist", artist);			
		    }
		
		    List<?> newObjects = newQuery.getResultList();
		    List<YahooArtistResult> newResults = new ArrayList<YahooArtistResult>(); 
		    for (Object o : newObjects) {
			    assert o != null;
			    newResults.add((YahooArtistResult) o);
		    }
		
		    if (newResults.isEmpty()) {
			    throw new NotFoundException("No yahoo artists were found for artist " + artist + " artistId " + artistId);
		    }
		    return newResults.get(0);
        } else {
            // if we didn't perform an update, it means that the original results were not empty	
	        logger.debug("Returning Yahoo artist result from database cache for artist {} artistId {}: {}", 
					     new Object[]{artist, artistId, results.get(0)});
			return results.get(0);
		}				
	}

}
