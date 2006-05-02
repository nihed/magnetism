package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import javax.annotation.EJB;
import javax.ejb.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.ArtistView;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExpandedArtistView;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.NowPlayingThemesBundle;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonMusicView;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.AmazonItemSearch;
import com.dumbhippo.services.YahooSearchWebServices;

@Stateless
public class MusicSystemInternalBean implements MusicSystemInternal {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicSystemInternalBean.class);
	
	// 2 days
	static private final int YAHOO_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 2;
	// 14 days since we aren't getting price information
	static private final int AMAZON_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
	// hour timeout to retry on failure
	static private final int FAILED_QUERY_TIMEOUT = 1000 * 60 * 60;
	
	// how long to wait on the search API call
	static private final int REQUEST_TIMEOUT = 1000 * 12;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private Configuration config;
	
	private ExecutorService threadPool;
	
	@PostConstruct
	public void init() {
		threadPool = Executors.newFixedThreadPool(8, new ThreadFactory() {
			private int nextThreadId = 0;
			
			public synchronized Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("music pool " + nextThreadId);
				nextThreadId += 1;
				return t;
			}
		});
	}
	
	public Track getTrack(Map<String, String> properties) {
		
		final Track key = new Track(properties);
		try {
			Track detached = runner.runTaskRetryingOnConstraintViolation(new Callable<Track>() {
				
				public Track call() throws Exception {
					Query q;
					
					q = em.createQuery("FROM Track t WHERE t.digest = :digest");
					q.setParameter("digest", key.getDigest());
					
					Track res;
					try {
						res = (Track) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						res = key;
						em.persist(res);
						
						// this is a fresh new track, so asynchronously fill in the Yahoo! search results
						hintNeedsRefresh(res);
					}
					
					return res;	
				}			
			});
			return em.find(Track.class, detached.getId());
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	private void addTrackHistory(final User user, final Track track, final Date now) {
		try {
			runner.runTaskRetryingOnConstraintViolation(new Callable<TrackHistory>() {
				
				public TrackHistory call() throws Exception {
					Query q;
					
					q = em.createQuery("FROM TrackHistory h WHERE h.user = :user " +
							"AND h.track = :track");
					q.setParameter("user", user);
					q.setParameter("track", track);
					
					TrackHistory res;
					try {
						res = (TrackHistory) q.getSingleResult();
						res.setLastUpdated(now);
						res.setTimesPlayed(res.getTimesPlayed() + 1);
					} catch (EntityNotFoundException e) {
						res = new TrackHistory(user, track);
						res.setLastUpdated(now);
						res.setTimesPlayed(1);
						em.persist(res);
					}
					
					return res;
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			// not reached
		}
	}
	
	public void setCurrentTrack(final User user, final Track track) {
		addTrackHistory(user, track, new Date());
	}
	 
	public void setCurrentTrack(User user, Map<String,String> properties) {
		// empty properties means "not listening to any track" - we always
		// keep the latest track with content, we don't set CurrentTrack to null
		if (properties.size() == 0)
			return;
		
		Track track = getTrack(properties);
		setCurrentTrack(user, track);
	}
	
	public void addHistoricalTrack(User user, Map<String,String> properties) {
		// for now there's no difference here, but eventually we might have the 
		// client supply some properties like the date of listening instead of 
		// pretending we "just" listened to this track.
		setCurrentTrack(user, properties);
	}
	
	private TrackHistory getCurrentTrack(Viewpoint viewpoint, User user) throws NotFoundException {
		List<TrackHistory> list = getTrackHistory(viewpoint, user, History.LATEST, 0, 1);
		if (list.isEmpty())
			throw new NotFoundException("No current track");
		return list.get(0);
	}

	enum History {
		LATEST,
		FREQUENT
	}
	
	private List<TrackHistory> getTrackHistory(Viewpoint viewpoint, User user, History type, int firstResult, int maxResults) {
		//logger.debug("getTrackHistory() type {} for {} max results " + maxResults, type, user);
		
		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user) && maxResults != 1) {
			// A non-friend can only see one result
			maxResults = 1;
		}

		if (!identitySpider.getMusicSharingEnabled(user)) {
			return Collections.emptyList();
		}
		
		Query q;
		
		String order = null;
		switch (type) {
		case LATEST:
			order = " ORDER BY h.lastUpdated DESC ";
			break;
		case FREQUENT:
			// we need the secondary order of lastUpdated, or you get 
			// random choices from among tracks with same timesPlayed
			order = " ORDER BY h.timesPlayed DESC, h.lastUpdated DESC ";
			break;
		}
		
		q = em.createQuery("FROM TrackHistory h WHERE h.user = :user " + 
				order);
		q.setParameter("user", user);
		q.setFirstResult(firstResult);
		q.setMaxResults(maxResults);
		
		List<TrackHistory> results = new ArrayList<TrackHistory>();
		List<?> rawResults = q.getResultList();
		for (Object o : rawResults) {
			TrackHistory h = (TrackHistory) o;
			if (h.getTrack() != null) // force-loads the track if it wasn't
				results.add(h);
			else
				logger.debug("Ignoring TrackHistory with null track");
		}

		return results;
	}
	
	private int countTrackHistory(Viewpoint viewpoint, User user) {
		if (!identitySpider.getMusicSharingEnabled(user)) {
			return 0;
		}
		
		Query q;
		
		q = em.createQuery("SELECT COUNT(*) FROM TrackHistory h WHERE h.user = :user ");
		q.setParameter("user", user);
		
		Object o = q.getSingleResult();
		int count = ((Number)o).intValue();
		
		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user) && count > 1) {
			// A non-friend can only see one result
			count = 1;
		}

		return count;		
	}

	private List<TrackHistory> getTrackHistory(Viewpoint viewpoint, Group group, History type, int maxResults) {
		//logger.debug("getTrackHistory() type {} for {} max results " + maxResults, type, group);

		// This is not very efficient now is it...
		
		Set<User> members = groupSystem.getUserMembers(viewpoint, group, MembershipStatus.ACTIVE);
		List<TrackHistory> results = new ArrayList<TrackHistory>();
		for (User m : members) {
			List<TrackHistory> memberHistory = getTrackHistory(viewpoint, m, type, 0, maxResults);
			results.addAll(memberHistory);
		}
		
		Comparator<TrackHistory> comparator;
		switch (type) {
		case FREQUENT:
			comparator = new Comparator<TrackHistory>() {
			
			// we want the list in descending order of frequency, so this is "backward"
			public int compare(TrackHistory a, TrackHistory b) {
				int aPlayed = a.getTimesPlayed();
				int bPlayed = b.getTimesPlayed();
				if (aPlayed > bPlayed)
					return -1;
				else if (aPlayed < bPlayed)
					return 1;
				else
					return 0;
			}	
		};
		break;
		case LATEST:
			comparator = new Comparator<TrackHistory>() {
			
			public int compare(TrackHistory a, TrackHistory b) {
				long aUpdated = a.getLastUpdated().getTime();
				long bUpdated = b.getLastUpdated().getTime();
				if (aUpdated > bUpdated)
					return -1;
				else if (aUpdated < bUpdated)
					return 1;
				else
					return 0;
			}	
		};
		
		break;
		default:
			comparator = null;
		break;
		}
		
		Collections.sort(results, comparator);
		
		if (results.size() > maxResults) {
			results.subList(maxResults, results.size()).clear();
		}
		
		return results;
	}

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
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			if (track != null)
				return musicSystem.getYahooSongResultsSync(track);
			
			return musicSystem.getYahooSongResultsSync(albumId);			
		}
	}

	static private class YahooSongDownloadTask implements Callable<List<YahooSongDownloadResult>> {

		private String songId;
		
		public YahooSongDownloadTask(String songId) {
			this.songId = songId;
		}
		
		public List<YahooSongDownloadResult> call() {
			logger.debug("Entering YahooSongDownloadTask thread for songId {}", songId);
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getYahooSongDownloadResultsSync(songId);
		}
	}

	// this is called from getYahooAlbumResultsAsync which is currently not used anywhere
	static private class YahooAlbumTask implements Callable<List<YahooAlbumResult>> {

		private String artistId;
		
		public YahooAlbumTask(String artistId) {
			this.artistId = artistId;
		}
		
		public List<YahooAlbumResult> call() {
			logger.debug("Entering YahooAlbumTask thread for artistId {}", artistId);
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getYahooAlbumResultsSync(artistId);
		}
	}
	
	static private class AmazonAlbumSearchTask implements Callable<AmazonAlbumResult> {
		
		private String album;
		private String artist;

		public AmazonAlbumSearchTask(String album, String artist) {
			this.album = album;
			this.artist = artist;
		}
		
		public AmazonAlbumResult call() {
			logger.debug("Entering AmazonAlbumSearchTask thread for album {} by artist {}", album, artist);				

			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getAmazonAlbumSync(album, artist);
		}
	}
	
	static private class GetTrackViewTask implements Callable<TrackView> {

		private Track track;
		private long lastListen;
		
		public GetTrackViewTask(Track track, long lastListen) {
			this.track = track;
			this.lastListen = lastListen;
		}
		
		public TrackView call() {
			logger.debug("Entering GetTrackViewTask thread for track {}", track);
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getTrackView(track, lastListen);
		}
	}

	static private class GetAlbumViewTask implements Callable<AlbumView> {

		private String album;
		private String artist;
		
		public GetAlbumViewTask(String album, String artist) {
			this.album = album;
			this.artist = artist;
		}
		
		public AlbumView call() {
			logger.debug("Entering GetAlbumViewTask thread for album {}, artist {}", album, artist);
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getAlbumView(album, artist);
		}
	}
	
	public void hintNeedsRefresh(Track track) {
		// called for side effect to kick off querying the results
		getTrackViewAsync(track, -1);
	}
	
	private List<YahooSongResult> updateSongResultsSync(List<YahooSongResult> oldResults, Track track) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
		List<YahooSongResult> newResults = ws.lookupSong(track.getArtist(), track.getAlbum(), track.getName(),
				track.getDuration(), track.getTrackNumber());
		
		// Track can be detached if we are calling this from YahooSongTask's
		// call() method which is executed in a separate thread, and thus
		// with a new session.
		if (!em.contains(track)) {
			track = em.find(Track.class, track.getId()); // reattach
		} 
		
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
			
			if (oldResults.isEmpty()) {
				YahooSongResult marker = new YahooSongResult();
				marker.setNoResultsMarker(true);
				marker.setLastUpdated(new Date(updateTime));
				em.persist(marker);
				track.addYahooSongResult(marker);
				// we don't need to return this marker though, returning empty oldResults is good enough
			}
			
			return oldResults;
		}
	
		List<YahooSongResult> results = new ArrayList<YahooSongResult>();
		// let's see if any of the new results are already in the YahooSongResult table,
		// if they are, we just need to connect the existing YahooSongResult with this track
		for (Iterator<YahooSongResult> i = newResults.iterator(); i.hasNext();) {
            YahooSongResult n = i.next();
			Query q = em.createQuery("SELECT song FROM YahooSongResult song WHERE song.songId = :songId");
			q.setParameter("songId", n.getSongId());	
			try {
				YahooSongResult song = (YahooSongResult)q.getSingleResult();
				logger.debug("adding song with songId {} to track {}", song.getSongId(), track);
				track.addYahooSongResult(song);
				results.add(song);
				i.remove();
			} catch (EntityNotFoundException e) {	
				continue;
			} catch (NonUniqueResultException e) {
				logger.warn("non-unique result based on song id {} in YahooSongResult table", n.getSongId(), e.getMessage());
				throw new RuntimeException(e);
			}
		}

		for (YahooSongResult old : oldResults) {
			boolean stillFound = false;
			if (!old.isNoResultsMarker()) {
			    for (YahooSongResult n : newResults) {
				    logger.debug("comparing: {} {}", n.getSongId(), old.getSongId());
				    if (n.getSongId().equals(old.getSongId())) {
					    // it's ok to remove an object from newResults because we break out of the
					    // loop right after it
					    newResults.remove(n);
					    old.update(n); // old is attached, so this gets saved
					    results.add(old);
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
		
		// remaining newResults weren't previously saved
		for (YahooSongResult n : newResults) {
			logger.debug("inserting song with songId {} into YahooSongResult table", n.getSongId());
			em.persist(n);
			track.addYahooSongResult(n);
			results.add(n);
		}
		
		return results;
	}
	
	public List<YahooSongResult> getYahooSongResultsSync(Track track) {
		
		// yahoo lookup requires these fields, so just bail without them
		if (track.getArtist() == null ||
				track.getAlbum() == null ||
				track.getName() == null) {
			logger.debug("Track {} missing artist album or name, can't get yahoo stuff", track);
			return Collections.emptyList();
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
			return updateSongResultsSync(results, track);
		} else {
			logger.debug("Returning Yahoo song results from database cache for track {}: {}", track, results);
			return results;
		}
	}

	private List<YahooSongResult> updateSongResultsSync(List<YahooSongResult> oldResults, YahooAlbumResult album) {
		
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
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
	    
	    boolean allSongsStored = yahooAlbum.isAllSongsStored();
	    boolean needNewQuery = !allSongsStored;
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
	
	private List<YahooSongDownloadResult> updateSongDownloadResultsSync(List<YahooSongDownloadResult> oldResults, String songId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
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
		for (YahooSongDownloadResult n : newResults) {
			em.persist(n);
			results.add(n);
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
	
	private List<YahooAlbumResult> updateAlbumResultsSync(List<YahooAlbumResult> oldResults, String artistId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
		List<YahooAlbumResult> newResults = ws.lookupAlbums(artistId);
		
		logger.debug("New album results for artistId {}: {}", artistId, newResults);
		
		// Match new results to old results with same artist id, updating the old row.
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
			// drops the no results marker, and any album no longer found
			if (!stillFound) {
				em.remove(old);
			}
		}
		// remaining newResults weren't previously saved
		for (YahooAlbumResult n : newResults) {
			em.persist(n);
			results.add(n);
		}
		
		return results;
	}
	
	public List<YahooAlbumResult> getYahooAlbumResultsSync(String artistId) {
		
		// we require the artistId field
		if (artistId == null) {
			logger.error("artistId is null when requesting yahoo album results for an artist id");
			throw new RuntimeException("artistId is null when requesting yahoo album results for an artist id");
		}
		
		Query q;
		
		q = em.createQuery("FROM YahooAlbumResult album WHERE album.artistId = :artistId");
		q.setParameter("artistId", artistId);
		
		List<YahooAlbumResult> results;
		List<?> objects = q.getResultList();
		results = new ArrayList<YahooAlbumResult>(); 
		for (Object o : objects) {
			results.add((YahooAlbumResult) o);
		}
		
		boolean needNewQuery = results.isEmpty();
		if (!needNewQuery) {
			// the outdated results, if any, could be a special magic result with the NoResultsMarker 
			// flag set, or could be real results
			long now = System.currentTimeMillis();
			for (YahooAlbumResult r : results) {
				if ((r.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
					needNewQuery = true;
					logger.debug("Outdated Yahoo album result, will need to renew the search for artistId {}", artistId);
					break;
				}
			}
		}
		
		if (needNewQuery) {
			// passing in the old results here is a race, since the db could have 
			// changed underneath us - but the worst case is that we just fail to
			// get results once in a while
			return updateAlbumResultsSync(results, artistId);
		} else {
			logger.debug("Returning Yahoo album results from database cache for artistId {}: {}", artistId, results);
			return results;
		}
	}
	
	
	public String getYahooArtistIdSync(String artist) {
		// this can be improved in the future to take extra info (like an album) and resolve
		// conflicts when the name matches multiple artists, but this is good enough for now
		
		// we require the artist field
		if (artist == null) {
			logger.error("Artist is null when requesting yahoo artist id for an artist");
			throw new RuntimeException("Artist is null when requesting yahoo artist id for an artist");
		}
		
		// use the album database first to see if we know of any albums by this artist
        // if so, we can get the artistId from the YahooAlbumResult and skip the web service query
		Query q;
		
		q = em.createQuery("FROM YahooAlbumResult album WHERE album.artist = :artist");
		q.setParameter("artist", artist);
		
		boolean needNewQuery = false;
		List<?> objects = q.getResultList();
		
		YahooAlbumResult result = null;
		if (objects.isEmpty()) {
			needNewQuery = true;
		} else {
			result = (YahooAlbumResult)objects.get(0);
		}
		
		if (!needNewQuery) {
			// see if the result happens to be outdated
			long now = System.currentTimeMillis();
			if ((result.getLastUpdated().getTime() + YAHOO_EXPIRATION_TIMEOUT) < now) {
				needNewQuery = true;
			    logger.debug("Outdated Yahoo album result, will need to renew the search for artist {}", artist);
			}
		}
		
		String artistId = null;
		
		if (needNewQuery) {
			// we are not storing this in the database, just get the artist id directly
			YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
			try {
			    artistId = ws.lookupArtistId(artist);
			} catch (NotFoundException e) {
				logger.debug("Artist {} was not found by Yahoo web service.", artist);
			}
		} else {
			artistId = result.getArtistId();
		}
		
		logger.debug("Returning artistId for artist {}: {}", artist, artistId);
		return artistId;
	}
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track) {
		FutureTask<List<YahooSongResult>> futureSong =
			new FutureTask<List<YahooSongResult>>(new YahooSongTask(track));
		threadPool.execute(futureSong);
		return futureSong;
	}

	public Future<List<YahooSongResult>> getYahooSongResultsAsync(String albumId) {
		FutureTask<List<YahooSongResult>> futureSong =
			new FutureTask<List<YahooSongResult>>(new YahooSongTask(albumId));
		threadPool.execute(futureSong);
		return futureSong;
	}	
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId) {
		FutureTask<List<YahooSongDownloadResult>> futureDownload =
			new FutureTask<List<YahooSongDownloadResult>>(new YahooSongDownloadTask(songId));
		threadPool.execute(futureDownload);
		return futureDownload;		
	}
	
	// currently not used
	public Future<List<YahooAlbumResult>> getYahooAlbumResultsAsync(String artistId) {
		FutureTask<List<YahooAlbumResult>> futureAlbum =
			new FutureTask<List<YahooAlbumResult>>(new YahooAlbumTask(artistId));
		threadPool.execute(futureAlbum);
		return futureAlbum;
	}
	
	public Future<AmazonAlbumResult> getAmazonAlbumAsync(String album, String artist) {
		FutureTask<AmazonAlbumResult> futureAlbum =
			new FutureTask<AmazonAlbumResult>(
			    new AmazonAlbumSearchTask(album, artist));
		threadPool.execute(futureAlbum);
		return futureAlbum;		
	}
	
	private AmazonAlbumResult albumResultQuery(String artist, String album) {
		Query q;
		
		q = em.createQuery("FROM AmazonAlbumResult album WHERE album.artist = :artist AND album.album = :album");
		q.setParameter("artist", artist);
		q.setParameter("album", album);
		
		try {
			return (AmazonAlbumResult) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		} 
	}	
		
	public AmazonAlbumResult getAmazonAlbumSync(final String album, final String artist) {	
		if (artist == null || album == null) {
			logger.debug("missing artist or album, not looking up album {} by artist {} on amazon", 
					     album, artist);
			return null;
		}
		
		AmazonAlbumResult result = albumResultQuery(artist, album);

		if (result != null) {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + AMAZON_EXPIRATION_TIMEOUT) < now)) {
				result = null;
			}
		}

		if (result != null) {
			logger.debug("Using amazon search result from database for album {} by artist {}: {}", 
					     new Object[]{album, artist, result});
			return result;
		}
		
		AmazonItemSearch search = new AmazonItemSearch(REQUEST_TIMEOUT);
		String amazonKey;
		final AmazonAlbumData data;
		try {
			amazonKey = config.getPropertyNoDefault(HippoProperty.AMAZON_ACCESS_KEY_ID);
		} catch (PropertyNotFoundException e) {
			amazonKey = null;
		}
		if (amazonKey != null)
			data = search.searchAlbum(amazonKey, artist, album);
		else
			data = null;
		
		try {
			AmazonAlbumResult detached = runner.runTaskRetryingOnConstraintViolation(new Callable<AmazonAlbumResult>() {
				
				public AmazonAlbumResult call() {
					AmazonAlbumResult r = albumResultQuery(artist, album);
					if (r == null) {
						// data may be null, in which case the AmazonAlbumResult just 
						// caches that we have no result
						r = new AmazonAlbumResult(artist, album, data);
						r.setLastUpdated(new Date());
						em.persist(r);
					} else {
						if (data != null)
							r.updateData(data);
						else 
							; // if we got no data, just keep whatever the old data was
						r.setLastUpdated(new Date());
					}
					return r;
				}
				
			});
			
			result = em.find(AmazonAlbumResult.class, detached.getId());
		} catch (Exception e) {
			result = null;
		}
		
		if (result != null)
			logger.debug("Using new amazon search result for album {} by artist {}: {}", 
				     new Object[]{album, artist, result});
		return result;
	}
	
	private void fillAlbumInfo(Future<AmazonAlbumResult> futureAlbum, AlbumView albumView) {
		try {
			// set defaults for the image
			albumView.setSmallImageUrl(config.getProperty(HippoProperty.BASEURL) + "/images/no_image_available75x75light.gif");
			albumView.setSmallImageWidth(75);
			albumView.setSmallImageHeight(75);
			
			// now get the real stuff
			AmazonAlbumResult album;
			try {
				album = futureAlbum.get();
			} catch (InterruptedException e) {
				logger.warn("amazon album get thread interrupted {}", e.getMessage());
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				logger.warn("amazon album get thread execution exception {}", e.getMessage());
				throw new RuntimeException(e);
			}
			if (album != null) {
				if (album.getSmallImageUrl() != null) {
					albumView.setSmallImageUrl(album.getSmallImageUrl());
					albumView.setSmallImageWidth(album.getSmallImageWidth());
					albumView.setSmallImageHeight(album.getSmallImageHeight());
				}
				// sometimes we get an empty album view passed in, in which case we also
				// want to fill out the album title and artist name
				if (albumView.getTitle() == null) {
					albumView.setTitle(album.getAlbum());
				}	
				if (albumView.getArtist() == null) {
					albumView.setArtist(album.getArtist());
				}				
			}
		} catch (Exception e) {
			logger.debug("Failed to get Amazon album information", e);
		}
	}
	
	private void fillAlbumInfo(Future<AmazonAlbumResult> futureAlbum, Future<List<YahooSongResult>> futureAlbumTracks, AlbumView albumView) {
			fillAlbumInfo(futureAlbum, albumView);

			List<YahooSongResult> albumTracks;
			TreeMap<Integer, TrackView> sortedTracks = new TreeMap<Integer, TrackView>();
			try {
				albumTracks = futureAlbumTracks.get();
				// now we have a fun task of sorting these YahooSongResult by track number and filtering out
				// the ones with -1 or 0 track number (YahooSongResult has -1 track number by default, 
				// and 0 track number if it was returned set to that by the web service, which is typical
				// for songs for which track number is inapplicable or unknown)				
				// TODO: we might consider not storing songs with -1 or 0 track number in the database
				// when we are doing updateYahooSongResults based on an albumId, yet we might still
				// have some songs with -1 or 0 track number because of getting yahoo songs based on
				// tracks that were played (note: for Track objects -1 indicates that the track number
				// is inapplicable or unknown and 0 is not a valid value)
				// TODO: this can also be part of a threaded task
				for (YahooSongResult albumTrack : albumTracks) {
					if (albumTrack.getTrackNumber() > 0) {
						TrackView trackView = new TrackView(albumTrack.getName(), 
                                                            albumView.getTitle(),
                                                            albumView.getArtist(),
                                                            albumTrack.getDuration(),
                                                            albumTrack.getTrackNumber());
						sortedTracks.put(albumTrack.getTrackNumber(), trackView);
					}
				}
			} catch (InterruptedException e) {
				logger.warn("Thread interrupted getting tracks for an album from yahoo {}", e.getMessage());
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				logger.warn("Exception getting tracks for an album from yahoo {}", e.getMessage());
				throw new RuntimeException(e);
			}
			
			for (TrackView trackView : sortedTracks.values()) {
				albumView.addTrack(trackView);
			}
	}
	
	public TrackView getTrackView(Track track) {
		return getTrackView(track, -1);
	}
	
	public TrackView getTrackView(Track track, long lastListen) {
		TrackView view = new TrackView(track);
		if (lastListen >= 0)
			view.setLastListenTime(lastListen);

		// this method should never throw due to Yahoo or Amazon failure;
		// we should just return a view without the extra information.
		
		Future<AmazonAlbumResult> futureAlbum = getAmazonAlbumAsync(track.getAlbum(), track.getArtist());
		try {
			// get our song IDs; no point doing it async...
			List<YahooSongResult> songs = getYahooSongResultsSync(track);
			// start a thread to get each download url
			List<Future<List<YahooSongDownloadResult>>> downloads = new ArrayList<Future<List<YahooSongDownloadResult>>>(); 
			for (YahooSongResult song : songs) {
				if (!song.isNoResultsMarker()) {
				    downloads.add(getYahooSongDownloadResultsAsync(song.getSongId()));
				}
			}
			for (Future<List<YahooSongDownloadResult>> futureDownloads : downloads) {
				List<YahooSongDownloadResult> ds;
				try {
					ds = futureDownloads.get();
				} catch (InterruptedException e) {
					logger.warn("Thread interrupted getting song download info from yahoo {}", e.getMessage());
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					logger.warn("Exception getting song download info from yahoo {}", e.getMessage());
					throw new RuntimeException(e);
				}

				for (YahooSongDownloadResult d : ds) {
					if (d.isNoResultsMarker()) {
						// normally, if there is a no results marker result, there won't be other results
						continue;
					}
					// if two search results are for the same source, first is assumed better
					if (view.getDownloadUrl(d.getSource()) == null) {
						view.setDownloadUrl(d.getSource(), d.getUrl());
						//logger.debug("adding download url for {}", d.getSource().getYahooSourceName());
					} else {
						logger.debug("ignoring second download url for {}", d.getSource().getYahooSourceName());
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to get Yahoo! search information for TrackView {}: {}", view, e.getMessage());
		}
		fillAlbumInfo(futureAlbum, view.getAlbumView());
		return view;
	}

	public AlbumView getAlbumView(String album, String artist) {
		// this method should never throw due to Yahoo or Amazon failure;
		// we should just return a view without the extra information.
		
		Future<AmazonAlbumResult> futureAlbum = getAmazonAlbumAsync(album, artist);
		AlbumView view = new AlbumView(album, artist);
		fillAlbumInfo(futureAlbum, view);
		return view;
	}
	
	public AlbumView getAlbumView(Future<AmazonAlbumResult> futureAlbum) {
		// this method should never throw due to Yahoo or Amazon failure;
		// we should just return a view without the extra information.
		
		AlbumView view = new AlbumView();
		fillAlbumInfo(futureAlbum, view);
		return view;
	}
	
	public AlbumView getAlbumView(Future<AmazonAlbumResult> futureAlbum, Future<List<YahooSongResult>> futureAlbumTracks) {
		AlbumView view = new AlbumView();
		fillAlbumInfo(futureAlbum, futureAlbumTracks, view);
		return view;		
	}
	
	public ArtistView getArtistView(Track track) {
		// right now ArtistView has no info in it, so this isn't too complex
		ArtistView view = new ArtistView(track.getArtist());
		return view;
	}
	
	public ExpandedArtistView getExpandedArtistView(String artist, String artistId) throws NotFoundException {
		ExpandedArtistView view = new ExpandedArtistView(artist);
		if (artistId == null) {  		
            artistId = getYahooArtistIdSync(artist);				
		}
		
		if (artistId == null) {
			 throw new NotFoundException("Artist " + artist + " was not found.");
		}

		// get albums using an artistId
		List<YahooAlbumResult> albums = getYahooAlbumResultsSync(artistId);
		// can start threads to get each album cover and to get songs for each album in parallel 
		Map<String, Future<AmazonAlbumResult>> futureAlbums = new HashMap<String, Future<AmazonAlbumResult>>();		
		Map<String, Future<List<YahooSongResult>>> futureTracks = new HashMap<String, Future<List<YahooSongResult>>>();		
		for (YahooAlbumResult yahooAlbum : albums) {
			if (!yahooAlbum.isNoResultsMarker()) {
			    futureAlbums.put(yahooAlbum.getAlbumId(), getAmazonAlbumAsync(yahooAlbum.getAlbum(), yahooAlbum.getArtist()));			
			    // getAlbumTracksAsync is responsible for doing all the sorting, so we get a clean sorted list here
			    futureTracks.put(yahooAlbum.getAlbumId(), getYahooSongResultsAsync(yahooAlbum.getAlbumId()));	
			}
		}		
		
		// futureAlbums and futureTracks key sets should be the same
		for (String albumId : futureAlbums.keySet()) {
			Future<AmazonAlbumResult> futureAlbum = futureAlbums.get(albumId);
			Future<List<YahooSongResult>> futureAlbumTracks = futureTracks.get(albumId);
			AlbumView albumView = getAlbumView(futureAlbum, futureAlbumTracks);	
			view.addAlbum(albumView);			
		}
		return view;
	}
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackView(current.getTrack(), current.getLastUpdated().getTime());
	}
	
	public Future<TrackView> getTrackViewAsync(Track track, long lastListen) {
		FutureTask<TrackView> futureView =
			new FutureTask<TrackView>(new GetTrackViewTask(track, lastListen));
		threadPool.execute(futureView);
		return futureView;
	}
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackViewAsync(current.getTrack(), current.getLastUpdated().getTime());
	}
	
	public Future<AlbumView> getAlbumViewAsync(String album, String artist) {
		FutureTask<AlbumView> futureView = 
			new FutureTask<AlbumView>(new GetAlbumViewTask(album, artist));
		threadPool.execute(futureView);
		return futureView;
	}

	public Future<ArtistView> getArtistViewAsync(Track track) {
		// right now ArtistView has no info in it, so this isn't too complex
		ArtistView view = new ArtistView(track.getArtist());
		
		// java std lib should have a subclass of Future that just isn't in the future,
		// but this is a fine hack for now
		FutureTask<ArtistView> futureView = new FutureTask<ArtistView>(new Runnable() {
			public void run() {
				// does nothing
			}
		},
		view);
		futureView.run(); // so we can call get()
		return futureView;
	}

	private List<TrackView> getTrackViewResults(List<Future<TrackView>> futureViews) {
		
		// now harvest all the results
		List<TrackView> views = new ArrayList<TrackView>(futureViews.size());
		for (Future<TrackView> fv : futureViews) {
			TrackView v;
			try {
				v = fv.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Future<TrackView> was interrupted: " + e.getMessage(), e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Future<TrackView> had execution exception: " + e.getMessage(), e);
			}
			
			assert v != null;
			
			views.add(v);
		}
		
		return views;
	}
	
	private List<TrackView> getViewsFromTrackHistories(List<TrackHistory> tracks) {
		
		logger.debug("Getting TrackViews from tracks list with {} items", tracks.size());
		
		// spawn a bunch of yahoo updater threads in parallel
		List<Future<TrackView>> futureViews = new ArrayList<Future<TrackView>>(tracks.size());
		for (TrackHistory t : tracks) {
			futureViews.add(getTrackViewAsync(t.getTrack(), t.getLastUpdated().getTime()));
		}
	
		return getTrackViewResults(futureViews);
	}

	private List<AlbumView> getAlbumViewsFromTracks(List<Track> tracks) {
		
		logger.debug("Getting AlbumViews from tracks list with {} items", tracks.size());
		
		// spawn threads in parallel
		List<Future<AlbumView>> futureViews = new ArrayList<Future<AlbumView>>(tracks.size());
		for (Track t : tracks) {
			futureViews.add(getAlbumViewAsync(t.getAlbum(), t.getArtist()));
		}
	
		// now harvest all the results
		List<AlbumView> views = new ArrayList<AlbumView>(tracks.size());
		for (Future<AlbumView> fv : futureViews) {
			AlbumView v;
			try {
				v = fv.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Future<AlbumView> was interrupted", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Future<AlbumView> had execution exception", e);
			}
			
			assert v != null;
			
			views.add(v);
		}
		
		return views;
	}

	private List<ArtistView> getArtistViewsFromTracks(List<Track> tracks) {
		
		logger.debug("Getting ArtistViews from tracks list with {} items", tracks.size());
		
		// spawn threads in parallel
		List<Future<ArtistView>> futureViews = new ArrayList<Future<ArtistView>>(tracks.size());
		for (Track t : tracks) {
			futureViews.add(getArtistViewAsync(t));
		}
	
		// now harvest all the results
		List<ArtistView> views = new ArrayList<ArtistView>(tracks.size());
		for (Future<ArtistView> fv : futureViews) {
			ArtistView v;
			try {
				v = fv.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Future<ArtistView> was interrupted", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Future<ArtistView> had execution exception", e);
			}
			
			assert v != null;
			
			views.add(v);
		}
		
		return views;
	}
	
	public void pageLatestTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		Query q = em.createQuery("SELECT h FROM TrackHistory h ORDER BY h.lastUpdated DESC");
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<?> results = q.getResultList();
		pageable.setResults(getViewsFromTrackHistories(TypeUtils.castList(TrackHistory.class, results)));
		
		// Doing an exact count is expensive, our assumption is "lots and lots"
		pageable.setTotalCount(pageable.getBound());
	}
	
	// FIXME right now this returns the latest songs globally, since that's the easiest thing 
	// to get, but I guess we could try to be fancier
	public List<TrackView> getPopularTrackViews(int maxResults) {
		Query q;
	
		// FIXME this should try to get only one track per user or something, but 
		// I can't figure out the sql in a brief attempt
		q = em.createQuery("FROM TrackHistory h ORDER BY h.lastUpdated DESC");
		q.setMaxResults(maxResults);
		
		List<TrackHistory> results = new ArrayList<TrackHistory>();
		List<?> rawResults = q.getResultList();
		for (Object o : rawResults) {
			TrackHistory h = (TrackHistory) o;
			if (h.getTrack() != null) // force-loads the track if it wasn't
				results.add(h);
			else
				logger.debug("Ignoring TrackHistory with null track");
		}
		
		return getViewsFromTrackHistories(results);
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getLatestTrackViews() for user {}", user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, 0, maxResults);
		return getViewsFromTrackHistories(history);
	}
	
	public void pageLatestTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable) {
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, pageable.getStart(), pageable.getCount());
		pageable.setResults(getViewsFromTrackHistories(history));
		pageable.setTotalCount(countTrackHistory(viewpoint, user));
	}
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getFrequentTrackViews() for user {}", user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.FREQUENT, 0, maxResults);
		
		return getViewsFromTrackHistories(history);
	}
	
	public void pageFrequentTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable) {
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.FREQUENT, pageable.getStart(), pageable.getCount());
		pageable.setResults(getViewsFromTrackHistories(history));
		pageable.setTotalCount(countTrackHistory(viewpoint, user));
	}

	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) {
		//logger.debug("getLatestTrackViews() for group {}", group);
		List<TrackHistory> history = getTrackHistory(viewpoint, group, History.LATEST, maxResults);
		
		return getViewsFromTrackHistories(history);
	}

	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) {
		//logger.debug("getFrequentTrackViews() for group {}", group);
		List<TrackHistory> history = getTrackHistory(viewpoint, group, History.FREQUENT, maxResults);
		
		return getViewsFromTrackHistories(history);
	}
	
	private void pageTrackViewsFromQuery(Query query, Pageable<TrackView> pageable) {
		query.setFirstResult(pageable.getStart());
		query.setMaxResults(pageable.getCount());
		
		List<?> objects = query.getResultList();
		List<Future<TrackView>> futureViews = new ArrayList<Future<TrackView>>(objects.size());
		for (Object o : objects) {
			futureViews.add(getTrackViewAsync((Track)o, -1));
		}
		
		pageable.setResults(getTrackViewResults(futureViews));
	}

	public void pageFrequentTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		pageTrackViewsFromQuery(em.createNamedQuery("trackHistoryMostPopularTracks"), pageable);
		
		// Doing an exact count is expensive, our assumption is "lots and lots"
		pageable.setTotalCount(pageable.getBound());
	}
	
	public void pageOnePlayTrackViews(Viewpoint viewpoint, Pageable<TrackView> pageable) {
		pageTrackViewsFromQuery(em.createNamedQuery("trackHistoryOnePlayTracks"), pageable);
		
		// Doing an exact count is expensive and hard, our assumption is "lots and lots"
		pageable.setTotalCount(pageable.getBound());
	}
	
	public void pageFrequentTrackViewsSince(Viewpoint viewpoint, Date since, Pageable<TrackView> pageable) {
		Query query = em.createNamedQuery("trackHistoryMostPopularTracksSince");
		query.setParameter("since", since);
		pageTrackViewsFromQuery(query, pageable);
		
		Query countQuery = em.createQuery("SELECT COUNT(DISTINCT h.track) FROM TrackHistory h WHERE h.lastUpdated >= :since");
		countQuery.setParameter("since", since);
		Object o = countQuery.getSingleResult();
		pageable.setTotalCount(((Number)o).intValue());
	}

	public List<AlbumView> getLatestAlbumViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getLatestAlbumViews() for user {}", user);
		
		// FIXME we don't really have a way of knowing how many TrackHistory we need to get maxResults
		// unique albums. For now, just heuristically ask for more results so we have a better shot
		// at getting several albums, but probably some real solution would be nice.
		// The case to think about is someone playing an entire album, so their last dozen 
		// tracks or so are all from the same album.
		
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, 0, maxResults*12);
		
		Set<String> albums = new HashSet<String>();
		List<Track> tracks = new ArrayList<Track>(history.size());
		for (TrackHistory h : history) {
			Track t = h.getTrack();
			if (!albums.contains(t.getAlbum())) {
				tracks.add(t);
				albums.add(t.getAlbum());
			}
			if (albums.size() >= maxResults)
				break;
		}
		return getAlbumViewsFromTracks(tracks);		
	}

	public List<ArtistView> getLatestArtistViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getLatestArtistViews() for user {}", user);
		
		// FIXME we don't really have a way of knowing how many TrackHistory we need to get maxResults
		// unique artists. For now, just heuristically ask for more results so we have a better shot
		// at getting several albums, but probably some real solution would be nice.
		// The case to think about is someone playing an entire album, so their last dozen 
		// tracks or so are all from the same album/artist.
		
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, 0, maxResults*12);
		
		Set<String> artists = new HashSet<String>();
		List<Track> tracks = new ArrayList<Track>(history.size());
		for (TrackHistory h : history) {
			Track t = h.getTrack();
			if (!artists.contains(t.getArtist())) {
				tracks.add(t);
				artists.add(t.getArtist());
			}
			if (artists.size() >= maxResults)
				break;
		}
		return getArtistViewsFromTracks(tracks);		
	}
	
	public void pageFriendsLatestTrackViews(UserViewpoint viewpoint, Pageable<TrackView> pageable) {
		// FIXME: We really want to do something along the lines of 
		// GROUP BY h.track ORDER BY max(h.lastUpdated), but that won't work
		// with MySQL 4, so we accept the possibility of duplicate tracks for 
		// the moment. The way to fix this is to use native SQL ... see the handling
		// of similar queries globally with the queries defined in TrackHistory.java
		Set<User> contacts = identitySpider.getRawUserContacts(viewpoint, viewpoint.getViewer());
		String where = "WHERE h.user IN " + getUserSetSQL(contacts);
		
		Query q = em.createQuery("SELECT h FROM TrackHistory h " + where + " ORDER BY h.lastUpdated DESC");
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<?> results = q.getResultList();
		
		pageable.setResults(getViewsFromTrackHistories(TypeUtils.castList(TrackHistory.class, results)));
		
		q = em.createQuery("SELECT COUNT(*) FROM TrackHistory h " + where);
		Object o = q.getSingleResult();
		pageable.setTotalCount(((Number)o).intValue()); 
	}
	
	private Query buildSongQuery(Viewpoint viewpoint, String artist, String album, String name, int maxResults) throws NotFoundException {
		int count = 0;
		if (artist != null)
			++count;
		if (name != null)
			++count;
		if (album != null)
			++count;
		
		if (count == 0)
			throw new NotFoundException("Search has no parameters");
		
		StringBuilder sb = new StringBuilder("FROM Track t WHERE ");
		if (artist != null) {
			sb.append(" t.artist = :artist ");
			--count;
			if (count > 0)
				sb.append(" AND ");
		}
		if (album != null) {
			sb.append(" t.album = :album ");
			--count;
			if (count > 0)
				sb.append(" AND ");
		}
		if (name != null) {
			sb.append(" t.name = :name ");
			--count;
		}		
		if (count != 0)
			throw new RuntimeException("broken code in song search");
		
		// just to make this deterministic, though we have no real reason
		// to prefer one Track over another, this means we always use the 
		// same (earliest-created) row
		sb.append(" ORDER BY t.id");
		
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
	
	private Track getMatchingTrack(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException {
		// max results of 1 picks one of the matching Track arbitrarily
		Query q = buildSongQuery(viewpoint, artist, album, name, 1);
		
		try {
			Track t = (Track) q.getSingleResult();
			return t;
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("No matching track", e);
		}
	}

	private List<Track> getMatchingTracks(Viewpoint viewpoint, String artist, String album, String name) {
		Query q;
		try {
			 q = buildSongQuery(viewpoint, artist, album, name, -1);
		} catch (NotFoundException e) {
			return Collections.emptyList();
		}
		
		List<?> tracks = q.getResultList();
		List<Track> ret = new ArrayList<Track>(tracks.size());
		for (Object o : tracks) {
			ret.add((Track) o);
		}
		return ret;
	}
	
	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException {
		//logger.debug("song search artist " + artist + " album " + album + " name " + name);
        //TODO: eventually, this song search should not limit itself to only existing tracks, 
		// but get matching songs from the web service 
		return getTrackView(getMatchingTrack(viewpoint, artist, album, name));
	}
	
	public AlbumView albumSearch(Viewpoint viewpoint, String artist, String album) throws NotFoundException {
		try {
		    Track track = getMatchingTrack(viewpoint, artist, album, null);		
		    return getAlbumView(track.getAlbum(), track.getArtist());
		} catch (NotFoundException e) {
			// if there was no matching track for the album, we still should be able to display the album
			return getAlbumView(album, artist);
		}
	}

	public ArtistView artistSearch(Viewpoint viewpoint, String artist) throws NotFoundException {
		Track track = getMatchingTrack(viewpoint, artist, null, null);
		
		return getArtistView(track);
	}

	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist) throws NotFoundException {
        String artistId = null;
		// first try to find a matching track among existing tracks, if it exists, get an artist id
		// associated with one of its songs
        // finding a matching track helps us ensure that we will display the right artist
		try {
	        Track track = getMatchingTrack(viewpoint, artist, null, null);
	        // we could just do track.getYahooSongResults(), but why don't we
	        // check if any YahooSongResult cache updating is in order 
			List<YahooSongResult> songs = getYahooSongResultsSync(track);
			if (!songs.isEmpty()) {
			    artistId = songs.get(0).getArtistId();
			}		
		} catch (NotFoundException e) {
			logger.debug("Did not find a matching track for an artist {} locally", artist);
		}
        return getExpandedArtistView(artist, artistId);
	}
	
	private static final int MAX_RELATED_FRIENDS_RESULTS = 5;
	private static final int MAX_RELATED_ANON_RESULTS = 5;
	private static final int MAX_SUGGESTIONS_PER_FRIEND = 3;
	
	private enum RelatedType {
		ALBUMS,
		TRACKS,
		ARTISTS
	}
	
	private String getUserSetSQL(Set<User> users) {
		StringBuilder sb = new StringBuilder("(");
		
		for (User u : users) {
			sb.append("'");
			sb.append(u.getId());
			sb.append("',");
		}
		if (sb.charAt(sb.length()-1) == ',') {
			sb.setLength(sb.length() - 1);
		}
		sb.append(")");
		
		return new String(sb);
	}
	
	private List<PersonMusicView> getRelatedPeople(Viewpoint viewpoint, String artist, String album, String name, RelatedType type) {

		if (viewpoint == null)
			throw new IllegalArgumentException("System view not supported here");
		
		List<PersonMusicView> ret = new ArrayList<PersonMusicView>();
		
		List<Track> tracks = getMatchingTracks(viewpoint, artist, album, name);
		if (tracks.size() == 0)
			return ret;

		// FIXME Query.setParameter(List<Track>) doesn't seem to work, a hibernate bug?
		// also, we could merge this with the query to get the matching tracks and be
		// more efficient, but too lazy right now
		StringBuilder sb = new StringBuilder("FROM TrackHistory h WHERE h.track.id IN (");
		for (Track t : tracks) {
			sb.append(t.getId());
			sb.append(",");
		}
		if (sb.charAt(sb.length()-1) == ',') {
			sb.setLength(sb.length() - 1);
		}
		sb.append(")");
		
		Set<User> contacts = null;
		if (viewpoint instanceof UserViewpoint) {
			contacts = identitySpider.getRawUserContacts(viewpoint, ((UserViewpoint)viewpoint).getViewer());

			// disabled for now since we want to return anonymous recommendations too
			if (true && false) {
				sb.append(" AND h.user.id IN ");
				sb.append(getUserSetSQL(contacts));
			}
		}
		
		Query q = em.createQuery(sb.toString());
		
		Map<User,PersonMusicView> views = new HashMap<User,PersonMusicView>();
		
		int contactViews = 0;
		int anonViews = 0;
		
		// FIXME this is more parallelizable than it is here, i.e. we could
		// first collect all the Track then convert each one to TrackView all in 
		// parallel
		
		List<?> history = q.getResultList();
		for (Object o : history) {
			TrackHistory h = (TrackHistory) o;
			
			User user = h.getUser();

			// If the viewer is the user themself, we want to include
			// them in the result, because that prevents our pages
			// from looking strangely empty, but we don't want the
			// recommendation lists, since that is just strange.
			boolean isSelf = viewpoint.isOfUser(user);
			
			PersonMusicView pmv = views.get(user);
			if (pmv == null) {
				if (isSelf || contacts.contains(user)) {
					if (contactViews < MAX_RELATED_FRIENDS_RESULTS) {
						pmv = new PersonMusicView(identitySpider.getPersonView(viewpoint, user));

						if (!isSelf) {
							switch (type) {
							case TRACKS: {
								List<TrackView> latest = getLatestTrackViews(viewpoint, user, MAX_SUGGESTIONS_PER_FRIEND);
								pmv.setTracks(latest);
							}
							break;
							case ALBUMS: {
								List<AlbumView> latest = getLatestAlbumViews(viewpoint, user, MAX_SUGGESTIONS_PER_FRIEND);
								pmv.setAlbums(latest);
							}
							break;
							case ARTISTS: {
								List<ArtistView> latest = getLatestArtistViews(viewpoint, user, MAX_SUGGESTIONS_PER_FRIEND);
								pmv.setArtists(latest);
							}
							break;
							}
						}
						++contactViews;
					}
				} else {
					if (anonViews < MAX_RELATED_ANON_RESULTS) {
						switch (type) {
						case TRACKS: {
							// get latest tracks from system view
							List<TrackView> latest = getLatestTrackViews(null, user, MAX_SUGGESTIONS_PER_FRIEND);
							if (latest.size() > 0) {
								pmv = new PersonMusicView(); // don't load a PersonView
								pmv.setTracks(latest);
								++anonViews;
							}
						} 
						break;
						case ALBUMS: {								
							List<AlbumView> latest = getLatestAlbumViews(null, user, MAX_SUGGESTIONS_PER_FRIEND);
							if (latest.size() > 0) {
								pmv = new PersonMusicView(); // don't load a PersonView
								pmv.setAlbums(latest);
								++anonViews;
							}
						}
						break;
						case ARTISTS: {
							List<ArtistView> latest = getLatestArtistViews(null, user, MAX_SUGGESTIONS_PER_FRIEND);
							if (latest.size() > 0) {
								pmv = new PersonMusicView(); // don't load a PersonView
								pmv.setArtists(latest);
								++anonViews;
							}				
						}
						break;
						}
					}
				}
				
				if (pmv != null)
					views.put(user, pmv);
			}
			
			if (contactViews >= MAX_RELATED_FRIENDS_RESULTS &&
					anonViews >= MAX_RELATED_ANON_RESULTS)
				break;
		}
		
		ret.addAll(views.values());
		
		return ret;
	}

	public List<PersonMusicView> getRelatedPeopleWithTracks(Viewpoint viewpoint, String artist, String album, String name) {
		return getRelatedPeople(viewpoint, artist, album, name, RelatedType.TRACKS);
	}

	public List<PersonMusicView> getRelatedPeopleWithAlbums(Viewpoint viewpoint, String artist, String album, String name) {
		return getRelatedPeople(viewpoint, artist, album, name, RelatedType.ALBUMS);
	}

	public List<PersonMusicView> getRelatedPeopleWithArtists(Viewpoint viewpoint, String artist, String album, String name) {
		return getRelatedPeople(viewpoint, artist, album, name, RelatedType.ARTISTS);
	}
	
	public NowPlayingTheme getCurrentNowPlayingTheme(User user) throws NotFoundException {
		NowPlayingTheme theme = user.getAccount().getNowPlayingTheme();
		
		if (theme != null && theme.isDraft()) {
			logger.warn("User {} got a draft theme {} as their current one", user, theme);
			theme = null;
		}
		
		// try to pick a default FIXME put it in the config or something, not just "oldest theme"
		if (theme == null) {
			Query q = em.createQuery("FROM NowPlayingTheme WHERE draft=0 ORDER BY creationDate ASC");
			q.setMaxResults(1);
			try {
				theme = (NowPlayingTheme) q.getSingleResult();
			} catch (EntityNotFoundException e) {
				theme = null;
			}
		}

		if (theme == null)
			throw new NotFoundException("user has no now playing theme set and no default in the db");
		
		return theme;
	}
	
	public void setCurrentNowPlayingTheme(UserViewpoint viewpoint, User user, NowPlayingTheme theme) {
		if (!viewpoint.getViewer().equals(user))
			throw new RuntimeException("not allowed to set someone else's now playing theme");
		if (!em.contains(user))
			throw new RuntimeException("user is detached");
		if (!em.contains(theme))
			throw new RuntimeException("theme is detached");

		user.getAccount().setNowPlayingTheme(theme);
	}
	
	private NowPlayingTheme internalLookupNowPlayingTheme(String id) throws NotFoundException {
		NowPlayingTheme obj = em.find(NowPlayingTheme.class, id);
		if (obj == null)
			throw new NotFoundException("no now playing theme " + id);
		return obj;		
	}
	
	public NowPlayingTheme lookupNowPlayingTheme(String id) throws ParseException, NotFoundException {
		Guid.validate(id); // so we throw Parse instead of GuidNotFound if invalid
		return internalLookupNowPlayingTheme(id);
	}
	
	public NowPlayingTheme lookupNowPlayingTheme(Guid id) throws NotFoundException {
		return internalLookupNowPlayingTheme(id.toString());
	}
	
	public void setNowPlayingThemeImage(UserViewpoint viewpoint, String id, String type, String shaSum) throws NotFoundException, ParseException {
		User user = viewpoint.getViewer();
		NowPlayingTheme theme = lookupNowPlayingTheme(id);
		if (!theme.getCreator().equals(user))
			throw new NotFoundException("unauthorized user editing theme");
		if (type.equals("active"))
			theme.setActiveImage(shaSum);
		else if (type.equals("inactive"))
			theme.setInactiveImage(shaSum);
		else
			throw new RuntimeException("unknown theme image type '" + type + "'");
	} 
	
	public NowPlayingThemesBundle getNowPlayingThemesBundle(Viewpoint viewpoint, User user) {
		
		// For now the viewpoint doesn't matter unless it's a "draft" theme
		// in which case you can only see your own stuff
		
		NowPlayingThemesBundle bundle = new NowPlayingThemesBundle();
		Set<NowPlayingTheme> alreadyUsed = new HashSet<NowPlayingTheme>();
		
		NowPlayingTheme current;
		try {
			current = getCurrentNowPlayingTheme(user);
		} catch (NotFoundException e) {
			current = null;
		}
		
		if (current != null) {
			bundle.setCurrentTheme(current);
			alreadyUsed.add(current);
		}

		boolean viewingSelf = viewpoint.isOfUser(user);
		
		if (viewingSelf) {
			// this query will include our draft themes
			Query q = em.createQuery("FROM NowPlayingTheme t WHERE t.creator=:creator ORDER BY creationDate DESC");
			q.setParameter("creator", user);
			List<?> myThemes = q.getResultList();
			List<NowPlayingTheme> myThemesFiltered = new ArrayList<NowPlayingTheme>();
			for (Object o : myThemes) {
				if (!alreadyUsed.contains((NowPlayingTheme) o)) {
					myThemesFiltered.add((NowPlayingTheme) o);
					alreadyUsed.add((NowPlayingTheme) o);
				}
			}
			bundle.setMyThemes(myThemesFiltered);
		}

		// this will return no friends if we can't see this person's contacts
		// from our viewpoint
		Set<Contact> friends = identitySpider.getRawContacts(viewpoint, user);

		if (!friends.isEmpty()) {	
			String draftClause; 
			if (viewpoint instanceof SystemViewpoint)
				draftClause = null;
			else if (viewpoint instanceof UserViewpoint)
				draftClause = "(t.draft=0 OR t.creator=:viewer)";
			else
				draftClause = "(t.draft=0)";
			
			StringBuilder sb = new StringBuilder("FROM NowPlayingTheme t ");
			sb.append("WHERE t.creator.id IN (");
			for (Contact c : friends) {
				User u = identitySpider.getUser(c);
				if (u != null) {
					sb.append("'");
					sb.append(u.getId());
					sb.append("'");
					sb.append(",");
				}
			}
			if (sb.charAt(sb.length()-1) == ',') {
				sb.setLength(sb.length() - 1);
			}
			sb.append(")");
			
			if (draftClause != null) {
				sb.append("AND ");
				sb.append(draftClause);
			}
			
			sb.append(" ORDER BY creationDate DESC");
			
			Query q = em.createQuery(sb.toString());
			if (viewpoint instanceof UserViewpoint) {			
				q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());
			}
			
			List<?> friendsThemes = q.getResultList();
			
			List<NowPlayingTheme> friendsThemesFiltered = new ArrayList<NowPlayingTheme>();
			for (Object o : friendsThemes) {
				if (!alreadyUsed.contains((NowPlayingTheme) o)) {
					friendsThemesFiltered.add((NowPlayingTheme) o);
					alreadyUsed.add((NowPlayingTheme) o);
				}
			}
			bundle.setFriendsThemes(friendsThemesFiltered);
		}
				
		/* Now pick 5 themes we don't have already, deterministically for now 
		 * FIXME should be random in some way
		 */
		
		StringBuilder sb = new StringBuilder("FROM NowPlayingTheme t WHERE t.draft=0 ");
		
		if (!alreadyUsed.isEmpty()) {
			sb.append(" AND t.id NOT IN (");
			for (NowPlayingTheme t : alreadyUsed) {
				sb.append("'");
				sb.append(t.getId());
				sb.append("'");
				sb.append(",");
			}
			if (sb.charAt(sb.length()-1) == ',') {
				sb.setLength(sb.length() - 1);
			}
			sb.append(")");
		}
		sb.append(" ORDER BY creationDate DESC");
		
		Query q = em.createQuery(sb.toString());
		q.setMaxResults(5);
		List<?> randomThemes = q.getResultList();
		
		List<NowPlayingTheme> randomThemesFiltered = new ArrayList<NowPlayingTheme>();
		for (Object o : randomThemes) {
			if (!alreadyUsed.contains((NowPlayingTheme) o)) {
				randomThemesFiltered.add((NowPlayingTheme) o);
				alreadyUsed.add((NowPlayingTheme) o);
			} else {
				throw new RuntimeException("we asked for themes we didn't already have, but got some we did");
			}
		}
		bundle.setRandomThemes(randomThemesFiltered);
		
		return bundle;
	}
	
	public List<NowPlayingTheme> getExampleNowPlayingThemes(Viewpoint viewpoint, int maxResults) {
		// FIXME pick certain good ones or something
		// FIXME EJBQL syntax is wrong, but don't have docs handy, will fix with the others
		Query q = em.createQuery("FROM NowPlayingTheme t WHERE t.draft=0");
		q.setMaxResults(maxResults); 
		return TypeUtils.castList(NowPlayingTheme.class, q.getResultList());
	}
	
	public NowPlayingTheme createNewNowPlayingTheme(UserViewpoint viewpoint, NowPlayingTheme basedOn) {
		if (basedOn != null && !em.contains(basedOn))
			basedOn = em.find(NowPlayingTheme.class, basedOn.getId()); // reattach
		NowPlayingTheme theme = new NowPlayingTheme(basedOn, viewpoint.getViewer());
		em.persist(theme);
		return theme;
	}
}
