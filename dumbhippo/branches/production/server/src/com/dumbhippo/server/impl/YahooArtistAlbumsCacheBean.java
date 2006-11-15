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
import com.dumbhippo.persistence.CachedYahooArtistAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.YahooArtistAlbumsCache;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
@Stateless
public class YahooArtistAlbumsCacheBean extends AbstractCacheBean<String,List<YahooAlbumData>> implements YahooArtistAlbumsCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooArtistAlbumsCacheBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	@EJB
	private Configuration config;
	
	public YahooArtistAlbumsCacheBean() {
		super(Request.YAHOO_ARTIST_ALBUMS);
	}
	
	static private class YahooArtistAlbumsTask implements Callable<List<YahooAlbumData>> {

		private String artistId;
		
		public YahooArtistAlbumsTask(String artistId) {
			this.artistId = artistId;
		}
		
		public List<YahooAlbumData> call() {			
			logger.debug("Entering YahooArtistAlbumsTask thread for artistId {}", artistId);

			// we do this instead of an inner class to work right with threads
			YahooArtistAlbumsCache cache = EJBUtil.defaultLookup(YahooArtistAlbumsCache.class);
			
			// Check again in case another node stored the data first
			List<YahooAlbumData> alreadyStored = cache.checkCache(artistId);
			if (alreadyStored != null)
				return alreadyStored;
			
			List<YahooAlbumData> result = cache.fetchFromNet(artistId);
			
			return cache.saveInCache(artistId, result);
		}
	}
	
	public List<YahooAlbumData> getSync(String artistId) {
		return getFutureResultEmptyListOnException(getAsync(artistId));
	}

	public Future<List<YahooAlbumData>> getAsync(String artistId) {
		if (artistId == null)
			throw new IllegalArgumentException("null artistId passed to YahooArtistAlbumsCacheBean");
		
		List<YahooAlbumData> result = checkCache(artistId);
		if (result != null) {
			logger.debug("Using cached album listing of {} items for artistId {}", result.size(), artistId);
			return new KnownFuture<List<YahooAlbumData>>(result);
		}
	
		return getExecutor().execute(artistId, new YahooArtistAlbumsTask(artistId));
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
		
		// This is both the lookup key and part of the returned results. 
		// Yahoo always returns the album with artistId = the artistId 
		// you searched for, though, so we don't need a separate column
		// for the artist id of the search vs. of the album.
		d.setArtistId(artistId);
		return d;
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<YahooAlbumData> saveInCache(final String artistId, List<YahooAlbumData> newAlbums) {
		// null albums doesn't happen but if it did would be the same as empty list
		if (newAlbums == null)
			newAlbums = Collections.emptyList();

		final List<YahooAlbumData> albums = newAlbums; 
		
		try {
			// if there were unique constraints involved here we'd need to retry on constraint
			// violations and also put in an em.flush after removing old rows			
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
							if (!s.getArtistId().equals(artistId)) {
								// if this happens, either change the yahoo web services stuff to filter it out,
								// or add a separate db column for the search key id vs. the returned id
								logger.warn("Yahoo returned an album for artistId {} with wrong artistId {}", artistId, s);
								continue;
							}
							
							CachedYahooArtistAlbumData d = createCachedAlbum(artistId);
							d.setLastUpdated(now);
							d.updateData(artistId, s);
							em.persist(d);
						}
					}
					
					return albums;
				}
				
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception {}: {}", e.getClass().getName(), e.getMessage());
				return albums;
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
	}

	private Query buildSearchQuery(String artist, String album, String name, int maxResults) throws NotFoundException {		
		int count = 0;
		if (artist != null)
			++count;
		if (name != null)
			++count;
		if (album != null)
			++count;
		
		if (count == 0)
			throw new NotFoundException("Search has no parameters");
		
		StringBuilder sb = new StringBuilder("SELECT album FROM CachedYahooArtistAlbumData album");
		if (name != null) {
			sb.append(", CachedYahooAlbumSongData song ");
		}
		sb.append(" WHERE ");
		if (artist != null) {
			sb.append(" album.artist = :artist ");
			--count;
			if (count > 0)
				sb.append(" AND ");
		}
		if (album != null) {
			sb.append(" album.album = :album ");
			--count;
			if (count > 0)
				sb.append(" AND ");
		}
		if (name != null) {
			sb.append(" song.name = :name AND album.albumId = song.albumId ");
			--count;
		}		
		if (count != 0)
			throw new RuntimeException("broken code in song search");		    	

		// we want to make this deterministic, so that we always return the same 
		// album for the same artist-album-song combination, we always use the 
		// same (earliest-created) row
		sb.append(" ORDER BY album.id");
		
		Query q = em.createQuery(sb.toString());
		if (artist != null)
			q.setParameter("artist", artist);
		if (album != null)
			q.setParameter("album", album);
		if (name != null)
			q.setParameter("name", name);
		
		if (maxResults >= 0)
			q.setMaxResults(maxResults);

		return q;
	}	
	
	public YahooAlbumData findAlreadyCachedAlbum(String artist, String album, String song) throws NotFoundException {
		Query q = buildSearchQuery(artist, album, song, 1);
		try {
			CachedYahooArtistAlbumData result = (CachedYahooArtistAlbumData) q.getSingleResult();
			if (result.isNoResultsMarker())
				throw new NotFoundException("Search returned no results marker (should this happen?)");
			return result.toData();
		} catch (NoResultException e) {
			throw new NotFoundException("search did not match anything in the database");
		}
	}
}
