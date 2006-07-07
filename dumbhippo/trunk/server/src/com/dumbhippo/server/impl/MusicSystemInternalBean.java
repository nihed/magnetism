package com.dumbhippo.server.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Hits;
import org.hibernate.lucene.DocumentBuilder;
import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.AccountFeed;
import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.RhapLink;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackFeedEntry;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.TrackType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.ArtistView;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.ExpandedArtistView;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonMusicPlayView;
import com.dumbhippo.server.PersonMusicView;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TrackIndexer;
import com.dumbhippo.server.TrackSearchResult;
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
	
	// 14 days
	static private final int RHAPLINK_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;
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
	
	private static ExecutorService threadPool;
	private static boolean shutdown = false;
	
	private synchronized static ExecutorService getThreadPool() {
		if (shutdown)
			throw new RuntimeException("getThreadPool() called after shutdown");
			
		if (threadPool == null) {
			threadPool = ThreadUtils.newCachedThreadPool("music pool");
		}
		
		return threadPool;
	}
	
	public static void shutdown() {
		synchronized (MusicSystemInternalBean.class) {
			shutdown = true;
			
			if (threadPool != null) {
				threadPool.shutdown();
				threadPool = null;
			}
		}	
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
			TrackIndexer.getInstance().index(detached.getId());
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

		if (!identitySpider.getMusicSharingEnabled(user, Enabled.AND_ACCOUNT_IS_ACTIVE)) {
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
		if (!identitySpider.getMusicSharingEnabled(user, Enabled.AND_ACCOUNT_IS_ACTIVE)) {
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

	static final private int MAX_GROUP_HISTORY_TRACKS = 200;
	static final private int MAX_GROUP_HISTORY_TRACKS_PER_MEMBER = 8;
	
	private void chopTrackHistory(List<TrackHistory> results, int firstResult, int maxResults) {
		// chop out before firstResult
		if (firstResult > 0) {
			results.subList(0, Math.min(firstResult, results.size())).clear();
		}
		
		// chop off the end of the list if it's longer than needed
		if (results.size() > maxResults) {
			results.subList(maxResults, results.size()).clear();
		}
	}
	
	private List<TrackHistory> getTrackHistory(Viewpoint viewpoint, Group group, History type, int firstResult, int maxResults) {
		List<TrackHistory> results = getEntireTrackHistory(viewpoint, group, type);
		chopTrackHistory(results, firstResult, maxResults);
		return results;
	}
	
	private List<TrackHistory> getEntireTrackHistory(Viewpoint viewpoint, Group group, History type) {
		//logger.debug("getTrackHistory() type {} for {} max results " + maxResults, type, group);

		// This is not very efficient now is it...
		
		Set<User> members = groupSystem.getUserMembers(viewpoint, group, MembershipStatus.ACTIVE);
		List<TrackHistory> results = new ArrayList<TrackHistory>();
		for (User m : members) {
			List<TrackHistory> memberHistory = getTrackHistory(viewpoint, m, type, 0,
							MAX_GROUP_HISTORY_TRACKS_PER_MEMBER);
			results.addAll(memberHistory);
			if (results.size() > MAX_GROUP_HISTORY_TRACKS)
				break; // enough! this arbitrarily ignores certain users, but oh well.
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

			if (track != null) {			
			    logger.debug("Obtained transaction in YahooSongTask thread for track {}", track);
			} else {
			    logger.debug("Obtained transaction in YahooSongTask thread for album id {}", albumId);				
			}
			
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

		    logger.debug("Obtained transaction in YahooSongDownloadTask thread for songId {}", songId);
		    
			return musicSystem.getYahooSongDownloadResultsSync(songId);
		}
	}

	// this is called from getYahooAlbumResultsAsync which is currently not used anywhere
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
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			logger.debug("Obtained transaction in YahooAlbumTask thread for artist {}", artist);			
			return musicSystem.getYahooAlbumResultsSync(artist, albumsByArtist, albumToExclude);
		}
	}

	static private class YahooAlbumSearchTask implements Callable<YahooAlbumResult> {
		
		private YahooSongResult yahooSong;

		public YahooAlbumSearchTask(YahooSongResult yahooSong) {
			this.yahooSong = yahooSong;
		}
		
		public YahooAlbumResult call() {
			logger.debug("Entering YahooAlbumSearchTask thread for yahooSong {}", yahooSong);				

			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);

			logger.debug("Obtained transaction in YahooAlbumSearchTask thread for yahooSong {}", yahooSong);	
			
			try {
			    YahooAlbumResult yahooAlbumResult = musicSystem.getYahooAlbumSync(yahooSong);
			    return yahooAlbumResult;
			} catch (NotFoundException e) {
				logger.debug(e.getMessage());
				return null;
			}
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

			logger.debug("Obtained transaction in AmazonAlbumSearchTask thread for album {} by artist {}", album, artist);	
			
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

			logger.debug("Obtained transaction in GetTrackViewTask thread for track {}", track);
			
			return musicSystem.getTrackView(track, lastListen);
		}
	}

	static private class GetAlbumViewTask implements Callable<AlbumView> {

		private Future<YahooAlbumResult> futureYahooAlbum;
		private Future<AmazonAlbumResult> futureAmazonAlbum;
		
		public GetAlbumViewTask(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum) {
			this.futureYahooAlbum = futureYahooAlbum;
			this.futureAmazonAlbum = futureAmazonAlbum;
		}
		
		public AlbumView call() {
			logger.debug("Entering GetAlbumViewTask thread for a futureYahooAlbum and a futureAmazonAlbum");
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			logger.debug("Obtained transaction in GetAlbumViewTask thread for a futureYahooAlbum and a futureAmazonAlbum");
			
			return musicSystem.getAlbumView(futureYahooAlbum, futureAmazonAlbum);
		}
	}
	
	public void hintNeedsRefresh(Track track) {
		// called for side effect to kick off querying the results
		getTrackViewAsync(track, -1);
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
				logger.warn("non-unique result based on song id {} in YahooSongResult table", n.getSongId(), e.getMessage());
				throw new RuntimeException(e);
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
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId) {
		FutureTask<List<YahooSongDownloadResult>> futureDownload =
			new FutureTask<List<YahooSongDownloadResult>>(new YahooSongDownloadTask(songId));
		getThreadPool().execute(futureDownload);
		return futureDownload;		
	}
	
	// currently not used
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
	
	public Future<AmazonAlbumResult> getAmazonAlbumAsync(String album, String artist) {
		FutureTask<AmazonAlbumResult> futureAlbum =
			new FutureTask<AmazonAlbumResult>(new AmazonAlbumSearchTask(album, artist));
		getThreadPool().execute(futureAlbum);
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
	
	private void fillAlbumInfo(YahooAlbumResult yahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum, AlbumView albumView) {
		try {			
			// Note that if neither Amazon nor Yahoo! has a small image url, we want to leave 
			// it null so AlbumView can default to our "no image" picture. This also 
			// means that albumView.getSmallImageUrl() never returns null
			
			// first see what we can get from yahoo album result
			if (yahooAlbum != null) {
			    albumView.setReleaseYear(yahooAlbum.getReleaseYear());
                if (yahooAlbum.getSmallImageUrl() != null) {
			        albumView.setSmallImageUrl(yahooAlbum.getSmallImageUrl());
			        albumView.setSmallImageHeight(yahooAlbum.getSmallImageHeight());
			        albumView.setSmallImageWidth(yahooAlbum.getSmallImageWidth());	
                }
				// sometimes we get an empty album view passed in, in which case we also
				// want to fill out the album title and artist name
				if (albumView.getTitle() == null) {
					albumView.setTitle(yahooAlbum.getAlbum());
				}	
				if (albumView.getArtist() == null) {
					albumView.setArtist(yahooAlbum.getArtist());            
				}           
			}
			
			// now get the amazon stuff
			AmazonAlbumResult amazonAlbum;
			try {
				amazonAlbum = futureAmazonAlbum.get();
			} catch (InterruptedException e) {
				logger.warn("amazon album get thread interrupted {}", e.getMessage());
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				logger.warn("amazon album get thread execution exception {}", e.getMessage());
				throw new RuntimeException(e);
			}
						
			if (amazonAlbum != null) {
				// if album artwork was not available from yahoo, we are after album artwork from amazon
				if (amazonAlbum.getSmallImageUrl() != null && (yahooAlbum == null || yahooAlbum.getSmallImageUrl() == null)) {
					albumView.setSmallImageUrl(amazonAlbum.getSmallImageUrl());
					albumView.setSmallImageWidth(amazonAlbum.getSmallImageWidth());
					albumView.setSmallImageHeight(amazonAlbum.getSmallImageHeight());
				}
					
				// in case we did not find a related yahoo album, but have a related amazon album 
				if (albumView.getTitle() == null) {
					albumView.setTitle(amazonAlbum.getAlbum());
				}	
				if (albumView.getArtist() == null) {
					albumView.setArtist(amazonAlbum.getArtist());
				}
				
				logger.debug("Amazon product URL is " + amazonAlbum.getProductUrl());
				
				// This is a quick hack for when what we are getting back from Amazon doesn't seem to work
				//if (amazonAlbum.getASIN() != null) {
				//	albumView.setProductUrl("http://www.amazon.com/gp/product/" + amazonAlbum.getASIN()); 
				//}
				
				// also drag along the product URL, so we can display that
				// TODO: handle this in a generic way that supports multiple providers, like downloads
				if (amazonAlbum.getProductUrl() != null) {
					albumView.setProductUrl(amazonAlbum.getProductUrl());
				}			
			}
			
		} catch (Exception e) {
			logger.debug("Failed to get album information", e);
		}
	}
	
	private void fillAlbumInfo(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum, AlbumView albumView) {
		YahooAlbumResult yahooAlbum = null;
	    if (futureYahooAlbum != null) {	
			try {
				yahooAlbum = futureYahooAlbum.get();
			} catch (InterruptedException e) {
				logger.error("yahoo album get thread interrupted {}", e.getMessage());
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
 				// it is ok to call fillAlbumInfo bellow with yahooAlbum being null
 				logger.error("yahoo album get thread execution exception {}", e.getMessage());
			}
	    }
		fillAlbumInfo(yahooAlbum, futureAmazonAlbum, albumView);				
	}
	
	private void fillAlbumInfo(Viewpoint viewpoint, YahooSongResult yahooSong, YahooAlbumResult yahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum, Future<List<YahooSongResult>> futureAlbumTracks, AlbumView albumView) {
			fillAlbumInfo(yahooAlbum, futureAmazonAlbum, albumView);

			List<YahooSongResult> albumTracks;
			TreeMap<Integer, TrackView> sortedTracks = new TreeMap<Integer, TrackView>();
			TreeMap<Integer, List<Future<List<YahooSongDownloadResult>>>> trackDownloads = 
				new TreeMap<Integer, List<Future<List<YahooSongDownloadResult>>>>();
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
					// so fun! now we get to pair up the multiple YahooSongResult into
					// a single track, and most importantly, get the download urls
					// based on each YahooSongResult and add them to the right albumTrack
					
					if (albumTrack.getTrackNumber() > 0) {
						
						// we only need one of the songs with a given track number to create a track view,
						// but we need to go through all song results to get all the downloads
						if (sortedTracks.get(albumTrack.getTrackNumber()) == null) {
					   	    TrackView trackView = new TrackView(albumTrack.getName(), 
                                                                albumView.getTitle(),
                                                                albumView.getArtist(),
                                                                albumTrack.getDuration(),
                                                                albumTrack.getTrackNumber());
						    // if we are displaying this album because of a particular song search, 
						    // we want to display this song initially expanded
                            // because a single track can have multiple valid YahooSongResults associated
						    // with it (e.g. for different downloads), we do not want to check the equality
						    // of yahooSong and albumTrack here, but rather the equality of the song
						    // names
						    if ((yahooSong != null) && yahooSong.getName().equals(albumTrack.getName())) {
                        	    trackView.setShowExpanded(true);
                            }
						    fillTrackViewWithTrackHistory(viewpoint, trackView);						
						    sortedTracks.put(albumTrack.getTrackNumber(), trackView);
						}
						
						if (trackDownloads.get(albumTrack.getTrackNumber()) == null) {
                        	ArrayList<Future<List<YahooSongDownloadResult>>> downloads =
                        		new ArrayList<Future<List<YahooSongDownloadResult>>>();
                        	// if the trackNumber was positive this can't be a no results marker,
                        	// so the song id should also not be null
                        	downloads.add(getYahooSongDownloadResultsAsync(albumTrack.getSongId()));
                        	trackDownloads.put(albumTrack.getTrackNumber(), downloads);
                        } else {
                        	trackDownloads.get(albumTrack.getTrackNumber()).add(getYahooSongDownloadResultsAsync(albumTrack.getSongId()));
                        }
					}
				}
				
        		for (Integer trackNumber : sortedTracks.keySet()) {
        			TrackView trackView = sortedTracks.get(trackNumber);       			
        			for (Future<List<YahooSongDownloadResult>> futureDownloads : trackDownloads.get(trackNumber)) {
						List<YahooSongDownloadResult> ds = futureDownloads.get();					        			
        			    for (YahooSongDownloadResult d : ds) {
						    if (d.isNoResultsMarker()) {
							    // normally, if there is a no results marker result, there won't be other results
							    break;
						    }
						    // if two search results are for the same source, first is assumed better
						    if (trackView.getDownloadUrl(d.getSource()) == null) {
							    trackView.setDownloadUrl(d.getSource(), d.getUrl());
							    //logger.debug("adding download url for {}", d.getSource().getYahooSourceName());
						    } else {
							    logger.debug("ignoring second download url for {}", d.getSource().getYahooSourceName());
						    }
        			    }
					}
    				String rhapsodyDownloadUrl = 
    					getRhapsodyDownloadUrl(trackView.getArtist(), trackView.getAlbum(), trackView.getTrackNumber());
    				if (rhapsodyDownloadUrl != null) {
    					trackView.setDownloadUrl(SongDownloadSource.RHAPSODY, rhapsodyDownloadUrl);
    				}
        		}							
			} catch (InterruptedException e) {
				logger.warn("Thread interrupted getting tracks or downloads for an album from yahoo {}", e.getMessage());
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				logger.warn("Exception getting tracks or downloads for an album from yahoo {}", e.getMessage());
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
		
		YahooSongResult yahooSong = null;
		Future<YahooAlbumResult> futureYahooAlbum = null;
		Future<AmazonAlbumResult> futureAmazonAlbum = getAmazonAlbumAsync(track.getAlbum(), track.getArtist());
		try {
			// get our song IDs; no point doing it async...
			List<YahooSongResult> songs = getYahooSongResultsSync(track);
			
			// start a thread to get each download url
			List<Future<List<YahooSongDownloadResult>>> downloads = new ArrayList<Future<List<YahooSongDownloadResult>>>(); 
			for (YahooSongResult song : songs) {
				if (!song.isNoResultsMarker()) {
				    downloads.add(getYahooSongDownloadResultsAsync(song.getSongId()));
				    // we need one of these song results to get an album info for the track
				    if (yahooSong == null) {
				    	yahooSong = song;
				    }
				}
			}
			// if yahooSong is not null, get a YahooAlbumResult for it, so we can get an album artwork for the track
			if (yahooSong != null) {
				futureYahooAlbum = getYahooAlbumAsync(yahooSong);
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
						break;
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
			
			// if futureYahooAlbum is not null, try to get a Rhapsody download URL for this song and that album
			if (futureYahooAlbum != null) {
				YahooAlbumResult yahooAlbum = null;
				try {
					yahooAlbum = futureYahooAlbum.get();
				} catch (InterruptedException e) {
					logger.error("yahoo album get thread interrupted {}", e.getMessage());
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
	 				logger.error("yahoo album get thread execution exception {}", e.getMessage());
				}
				if (yahooAlbum != null) {
					String rhapsodyDownloadUrl = 
						getRhapsodyDownloadUrl(yahooAlbum.getArtist(), yahooAlbum.getAlbum(), yahooSong.getTrackNumber());
					if (rhapsodyDownloadUrl != null) {
						view.setDownloadUrl(SongDownloadSource.RHAPSODY, rhapsodyDownloadUrl);
						//logger.debug("set rhapsody download url {}", rhapsodyDownloadUrl);
					}
				} else {
					logger.warn("yahooAlbum for {} was null in getTrackView", yahooSong.getName());
				}
			}
			fillAlbumInfo(futureYahooAlbum, futureAmazonAlbum, view.getAlbumView());
		} catch (Exception e) {
			logger.error("Failed to get Yahoo! search information for TrackView {}: {}", view, e.getMessage());
		}
		
		return view;
	}

	public AlbumView getAlbumView(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum) {
		// this method should never throw due to Yahoo or Amazon failure;
		// we should just return a view without the extra information.
		
		AlbumView view = new AlbumView();
		fillAlbumInfo(futureYahooAlbum, futureAmazonAlbum, view);
		return view;
	}
	
	public AlbumView getAlbumView(Viewpoint viewpoint, YahooSongResult yahooSong, YahooAlbumResult yahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum, Future<List<YahooSongResult>> futureAlbumTracks) {
		AlbumView view = new AlbumView();
		fillAlbumInfo(viewpoint, yahooSong, yahooAlbum, futureAmazonAlbum, futureAlbumTracks, view);
		return view;		
	}
	
	public ArtistView getArtistView(Track track) {
		// right now ArtistView has no info in it, so this isn't too complex
		ArtistView view = new ArtistView(track.getArtist());
		return view;
	}
	
	private void fillArtistInfo(YahooArtistResult yahooArtist, ExpandedArtistView artistView) {
		artistView.setTotalAlbumsByArtist(yahooArtist.getTotalAlbumsByArtist());
		// set defaults for the image
        artistView.setSmallImageUrl(config.getProperty(HippoProperty.BASEURL) + "/images/no_image_available75x75light.gif");
        artistView.setSmallImageWidth(75);
        artistView.setSmallImageHeight(75);
			
        if (yahooArtist != null) {
            if (yahooArtist.getSmallImageUrl() != null) {
                artistView.setSmallImageUrl(yahooArtist.getSmallImageUrl());
                artistView.setSmallImageWidth(yahooArtist.getSmallImageWidth());
                artistView.setSmallImageHeight(yahooArtist.getSmallImageHeight());
                artistView.setYahooMusicPageUrl(yahooArtist.getYahooMusicPageUrl());
            }			
        }
    }
	 
	private void fillAlbumsByArtist(Viewpoint viewpoint, YahooArtistResult yahooArtist, Pageable<AlbumView> albumsByArtist, 
			                        YahooAlbumResult albumToExclude, ExpandedArtistView view) {
		// get albums using an artistId
		List<YahooAlbumResult> albums = getYahooAlbumResultsSync(yahooArtist, albumsByArtist, albumToExclude);
		
		// we want to fill artist info after the above call, because we only might have the total albums
		// by artist set correctly after we get yahoo album results for the artist
		fillArtistInfo(yahooArtist, view);
		
		// can start threads to get each album cover and to get songs for each album in parallel 
		Map<String, Future<AmazonAlbumResult>> futureAmazonAlbums = new HashMap<String, Future<AmazonAlbumResult>>();		
		Map<String, Future<List<YahooSongResult>>> futureTracks = new HashMap<String, Future<List<YahooSongResult>>>();
		Map<String, YahooAlbumResult> yahooAlbums = new HashMap<String, YahooAlbumResult>();
		for (YahooAlbumResult yahooAlbum : albums) {
			if (!yahooAlbum.isNoResultsMarker()) {
			    futureAmazonAlbums.put(yahooAlbum.getAlbumId(), getAmazonAlbumAsync(yahooAlbum.getAlbum(), yahooAlbum.getArtist()));			
			    // getAlbumTracksAsync is responsible for doing all the sorting, so we get a clean sorted list here
			    futureTracks.put(yahooAlbum.getAlbumId(), getYahooSongResultsAsync(yahooAlbum.getAlbumId()));
			    yahooAlbums.put(yahooAlbum.getAlbumId(), yahooAlbum);
			}
		}		
		
		// all key sets should be the same
		for (String albumId : yahooAlbums.keySet()) {
			YahooAlbumResult yahooAlbum = yahooAlbums.get(albumId);
			Future<AmazonAlbumResult> futureAmazonAlbum = futureAmazonAlbums.get(albumId);
			Future<List<YahooSongResult>> futureAlbumTracks = futureTracks.get(albumId);
			AlbumView albumView = getAlbumView(viewpoint, null, yahooAlbum, futureAmazonAlbum, futureAlbumTracks);
			view.addAlbum(albumView);			
		}		
	}
	
	public ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, String artist, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		ExpandedArtistView view = new ExpandedArtistView(artist);

	    // this call would throw a NotFoundException if an artist was not found, and would never return null
        YahooArtistResult yahooArtist = getYahooArtistResultSync(artist, null);				

		if (yahooArtist.isNoResultsMarker()) {
			 throw new NotFoundException("Artist " + artist + " was not found.");
		}
		
        fillAlbumsByArtist(viewpoint, yahooArtist, albumsByArtist, null, view);
        
		return view;
	}
	
	public ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, YahooSongResult song, YahooAlbumResult album, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		
		// we require the album field
		if (album == null) {
			logger.error("album is null when requesting an expanded artist view with an album");
			throw new RuntimeException("album is null when requesting an expanded artist view with an album");
		}

		if (album.isNoResultsMarker()) {
			 throw new NotFoundException("Cannot provide an expanded artist view based on a no "
					                     + " results marker album.");
		}
		
		ExpandedArtistView view = new ExpandedArtistView(album.getArtist());		

	    // this call would throw a NotFoundException if an artist was not found, and would never return null
        YahooArtistResult yahooArtist = getYahooArtistResultSync(album.getArtist(), album.getArtistId());		
        
		if (yahooArtist.isNoResultsMarker()) {
			 throw new NotFoundException("Artist " + album.getArtist() + " artistId " 
					                     + album.getArtistId() + " was not found.");
		}
		
		if (albumsByArtist.getStart() == 0) {
            Future<AmazonAlbumResult> amazonAlbum = getAmazonAlbumAsync(album.getAlbum(), album.getArtist());
            Future<List<YahooSongResult>> albumTracks = getYahooSongResultsAsync(album.getAlbumId());

		    AlbumView albumView = getAlbumView(viewpoint, song, album, amazonAlbum, albumTracks);
	        view.addAlbum(albumView);			
		}
		
        fillAlbumsByArtist(viewpoint, yahooArtist, albumsByArtist, album, view);
	    
		return view;
	}
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackView(current.getTrack(), current.getLastUpdated().getTime());
	}
	
	public Future<TrackView> getTrackViewAsync(Track track, long lastListen) {
		FutureTask<TrackView> futureView =
			new FutureTask<TrackView>(new GetTrackViewTask(track, lastListen));
		getThreadPool().execute(futureView);
		return futureView;
	}
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackViewAsync(current.getTrack(), current.getLastUpdated().getTime());
	}
	
	public Future<AlbumView> getAlbumViewAsync(Future<YahooAlbumResult> futureYahooAlbum, Future<AmazonAlbumResult> futureAmazonAlbum) {
		FutureTask<AlbumView> futureView = 
			new FutureTask<AlbumView>(new GetAlbumViewTask(futureYahooAlbum, futureAmazonAlbum));
		getThreadPool().execute(futureView);
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
	
	private List<TrackView> getViewsFromTrackHistories(Viewpoint viewpoint, List<TrackHistory> tracks, boolean includePersonMusicPlay) {
		
		logger.debug("Getting TrackViews from tracks list with {} items", tracks.size());
		
		// spawn a bunch of yahoo updater threads in parallel
		Map<TrackHistory, Future<TrackView>> futureViews = new HashMap<TrackHistory, Future<TrackView>>(tracks.size());
		for (TrackHistory t : tracks) {
			futureViews.put(t, getTrackViewAsync(t.getTrack(), t.getLastUpdated().getTime()));
		}
	
        // now harvest all the results
		List<TrackView> views = new ArrayList<TrackView>(futureViews.size());
		for (TrackHistory t : tracks) {
			TrackView v;
			try {
				v = futureViews.get(t).get();
				if (includePersonMusicPlay) {
				    // add the person who made this "track history" 
		            List<PersonMusicPlayView> personMusicPlayViews = new ArrayList<PersonMusicPlayView>();
				    personMusicPlayViews.add(new PersonMusicPlayView(identitySpider.getPersonView(viewpoint, t.getUser()), t.getLastUpdated()));
				    v.setPersonMusicPlayViews(personMusicPlayViews);
				}
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

	private List<AlbumView> getAlbumViewsFromTracks(List<Track> tracks) {
		
		logger.debug("Getting AlbumViews from tracks list with {} items", tracks.size());
		
		// spawn threads in parallel
		List<Future<AlbumView>> futureViews = new ArrayList<Future<AlbumView>>(tracks.size());
		for (Track t : tracks) {
			Future<AmazonAlbumResult> futureAmazonAlbum = getAmazonAlbumAsync(t.getAlbum(), t.getArtist());			
			List<YahooSongResult> songs = getYahooSongResultsSync(t);
			Future<YahooAlbumResult> futureYahooAlbum = null;
		
			// if the first song stored is a no results marker, there must be no other songs 
			// associated with the track
		    if (!songs.isEmpty() && !songs.get(0).isNoResultsMarker()) {
				try {
		    	    YahooArtistResult yahooArtist = getYahooArtistResultSync(t.getArtist(), null);				
				    YahooSongResult yahooSong = null;
				
			        for (YahooSongResult songForTrack : songs) {
			        	if (songForTrack.getArtistId().equals(yahooArtist.getArtistId())) {
			    	    	yahooSong = songForTrack;
			    		    break;
			    	    }
			        }
		    	
	                if (yahooSong != null) { 	    
			            futureYahooAlbum = getYahooAlbumAsync(yahooSong);
	                }
				} catch (NotFoundException e) {
					//nothing to do, it's ok for futureYahooAlbum to be null
				}
		    }
		    
			futureViews.add(getAlbumViewAsync(futureYahooAlbum, futureAmazonAlbum));
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
		pageable.setResults(getViewsFromTrackHistories(viewpoint, TypeUtils.castList(TrackHistory.class, results), false));
		
		// Doing an exact count is expensive, our assumption is "lots and lots"
		pageable.setTotalCount(pageable.getBound());
	}
	
	// FIXME right now this returns the latest songs globally, since that's the easiest thing 
	// to get, but I guess we could try to be fancier
	public List<TrackView> getPopularTrackViews(Viewpoint viewpoint, int maxResults) {
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
		
		return getViewsFromTrackHistories(viewpoint, results, false);
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getLatestTrackViews() for user {}", user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, 0, maxResults);
		return getViewsFromTrackHistories(viewpoint, history, false);
	}
	
	public void pageLatestTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable) {
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, pageable.getStart(), pageable.getCount());
		pageable.setResults(getViewsFromTrackHistories(viewpoint, history, false));
		pageable.setTotalCount(countTrackHistory(viewpoint, user));
	}
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getFrequentTrackViews() for user {}", user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.FREQUENT, 0, maxResults);
		
		return getViewsFromTrackHistories(viewpoint, history, false);
	}
	
	public void pageFrequentTrackViews(Viewpoint viewpoint, User user, Pageable<TrackView> pageable) {
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.FREQUENT, pageable.getStart(), pageable.getCount());
		pageable.setResults(getViewsFromTrackHistories(viewpoint, history, false));
		pageable.setTotalCount(countTrackHistory(viewpoint, user));
	}

	public void pageLatestTrackViews(Viewpoint viewpoint, Group group, Pageable<TrackView> pageable) {
		List<TrackHistory> history = getEntireTrackHistory(viewpoint, group, History.LATEST);
		pageable.setTotalCount(history.size());
		chopTrackHistory(history, pageable.getStart(), pageable.getCount());
		pageable.setResults(getViewsFromTrackHistories(viewpoint, history, true));
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) {
		//logger.debug("getLatestTrackViews() for group {}", group);
		List<TrackHistory> history = getTrackHistory(viewpoint, group, History.LATEST, 0, maxResults);
		
		return getViewsFromTrackHistories(viewpoint, history, true);
	}

	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) {
		//logger.debug("getFrequentTrackViews() for group {}", group);
		List<TrackHistory> history = getTrackHistory(viewpoint, group, History.FREQUENT, 0, maxResults);
		
		return getViewsFromTrackHistories(viewpoint, history, false);
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
		
		// filter out ourselves
		Iterator<User> iterator = contacts.iterator();
		while (iterator.hasNext()) {
			User user = iterator.next();
			if (user.equals(viewpoint.getViewer()))
			    iterator.remove();
		}
 		
		if (contacts.size() == 0) { // h.user IN () isn't legal
			List<TrackView> empty = Collections.emptyList();
			pageable.setResults(empty);
			pageable.setTotalCount(0);
			return;
		}
		
		String where = "WHERE h.user IN " + getUserSetSQL(contacts);
				
		Query q = em.createQuery("SELECT h FROM TrackHistory h " + where + " ORDER BY h.lastUpdated DESC");
		q.setFirstResult(pageable.getStart());
		q.setMaxResults(pageable.getCount());
		List<?> results = q.getResultList();
		
		pageable.setResults(getViewsFromTrackHistories(viewpoint, TypeUtils.castList(TrackHistory.class, results), true));
		
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
		
		// we want to make this deterministic, so that we always return the same 
		// track for the same artist-album-song combination, we always use the 
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
	
	private Query buildAlbumQuery(Viewpoint viewpoint, String artist, String album, String name, int maxResults) throws NotFoundException {
		int count = 0;
		if (artist != null)
			++count;
		if (name != null)
			++count;
		if (album != null)
			++count;
		
		if (count == 0)
			throw new NotFoundException("Search has no parameters");
		
		StringBuilder sb = new StringBuilder("SELECT album FROM YahooAlbumResult album");
		if (name != null) {
			sb.append(", YahooSongResult song ");
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
		return getTrackView(getMatchingTrack(viewpoint, artist, album, name));
	}

	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
        // albumsByArtist is a pageable object that contains information on what albums the expanded
        // artist view should be loaded with, it also needs to have these albums set in its results field
		ExpandedArtistView artistView = getExpandedArtistView(viewpoint, artist, albumsByArtist);
		artistView.pageAlbums(albumsByArtist);
		return artistView;
	}
	
	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, String album, String name, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		if (artist== null && album == null && name == null) {
			throw new NotFoundException("Search has no parameters");
		}
		
		// if we only get an artist it's more fun to return a random album, rather than some album we already know about
		if (album == null && name == null) {
			return expandedArtistSearch(viewpoint, artist, albumsByArtist);
		}
	
		// for now, it's good if this function populates the album list of the expanded artist view with one
		// best-matching album, later it would also add in top (10) albums and a total number of albums for an artist
		try {
			Track track = getMatchingTrack(viewpoint, artist, album, name);
	        // we could just do track.getYahooSongResults(), but why don't we
	        // check if any YahooSongResult cache updating is in order 
			List<YahooSongResult> songs = getYahooSongResultsSync(track);
			if (!songs.isEmpty() && !songs.get(0).isNoResultsMarker()) {
				// get the primary artist result for this artist, than make sure we use the song result
				// associated with the primary artist result
				YahooArtistResult yahooArtist = getYahooArtistResultSync(track.getArtist(), null);
				
				YahooSongResult yahooSong = null;
				
			    for (YahooSongResult songForTrack : songs) {
			    	if (songForTrack.getArtistId().equals(yahooArtist.getArtistId())) {
			    		yahooSong = songForTrack;
			    		break;
			    	}
			    }
			    
			    if (yahooSong != null) {
				    // we want to get yahoo album here synchronously
                    YahooAlbumResult yahooAlbum = getYahooAlbumSync(yahooSong);	
                    // albumsByArtist is a pageable object that contains information on what albums the expanded
                    // artist view should be loaded with, it also needs to have these albums set in its results field
                    YahooSongResult songToExpand = null;
                    if (name != null) {
                	    //this is a search based on a song name, not just an album search
                	    songToExpand = yahooSong;
                    }
    			    ExpandedArtistView artistView = getExpandedArtistView(viewpoint, songToExpand, yahooAlbum, albumsByArtist);
    		 	    artistView.pageAlbums(albumsByArtist);
    		        return artistView;
			    }
			}		
		} catch (NotFoundException e) {
			// we get this exception if a matching track, a matching artist, or an album with a given id were not found 
			logger.debug(e.getMessage());
		}

	    // in other cases we have to do a query to get YahooAlbumResult
		// if we displayed it, we have to have it locally, but in order to update it or to fulfill
		// arbitrary user-defined searches we will need to use lookupSong, and then lookupAlbum for this song
		// (or rather with a given album id)		
		Query q = buildAlbumQuery(viewpoint, artist, album, name, 1);
			
		try {
			YahooAlbumResult yahooAlbum = (YahooAlbumResult)q.getSingleResult();
			ExpandedArtistView artistView = getExpandedArtistView(viewpoint, null, yahooAlbum, albumsByArtist);
			artistView.pageAlbums(albumsByArtist);
		    return artistView;
		} catch (EntityNotFoundException e) { 
			logger.debug("Did not find a cached yahoo result that matched artist {} album {} name {}", 
					     new Object[]{artist, album, name});
			throw new NotFoundException("Did not find a cached yahoo result that matched artist "
					                    + artist + " album " + album + " name " + name);
		} catch (NonUniqueResultException e) {
		    // this should not happen because we requested one result!
			logger.error("Received multiple yahoo album results when requesting one");
			throw new RuntimeException("Received multiple yahoo album results when requesting one");
		}				 
	}

	private static final int MAX_RELATED_FRIENDS_RESULTS = 5;
	private static final int MAX_RELATED_ANON_RESULTS = 5;
	private static final int MAX_SUGGESTIONS_PER_FRIEND = 3;
	private static final int MAX_RELATED_PEOPLE_RESULTS = 3;
	
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
	
	private void fillTrackViewWithTrackHistory(Viewpoint viewpoint, TrackView trackView) {

        List<Track> tracks = getMatchingTracks(viewpoint, trackView.getArtist(), trackView.getAlbum(), trackView.getName());
        if (tracks.size() == 0) {
        	trackView.setTotalPlays(0);
            // setting the number of friends who played this track is only relevant if we have a user viewpoint
            if (viewpoint instanceof UserViewpoint) {
        	    trackView.setNumberOfFriendsWhoPlayedTrack(0);
            }
            return;
        }

        StringBuilder sb = new StringBuilder("FROM TrackHistory h WHERE h.track.id IN (");
        for (Track t : tracks) {
            sb.append(t.getId());
            sb.append(",");
        }
        if (sb.charAt(sb.length()-1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append(")");

        Set<User> contacts = new HashSet<User>();

        if (viewpoint instanceof UserViewpoint) {
        	// we will not limit the query to h.user.id being in the contacts list, but we
        	// will give a preference to people in that list when constructing the list to return
            contacts = identitySpider.getRawUserContacts(viewpoint, ((UserViewpoint)viewpoint).getViewer());
        }

        // we want to desplay the most recent plays
        sb.append(" ORDER BY h.lastUpdated DESC");
        
        Query q = em.createQuery(sb.toString());

        // views contains results that we'll definitely want to display
        Map<User, PersonMusicPlayView> views = new HashMap<User, PersonMusicPlayView>();
        
        // extraViews is used only if we have a user viewpoint, and are trying to fill in views
        // with user's friends only, but might supplement them in the end with the extra views 
        // to return more results
        Map<User, PersonMusicPlayView> extraViews = new HashMap<User, PersonMusicPlayView>();
        
        // we keep this list of encountered contacts to be able to calculate the number of
        // contacts who played this track, to make sure that we count each contact only once,
        // and to do this counting separately from compiling the views map that should only
        // contain results we'll definitely want to display
        List<User> encounteredContacts = new ArrayList<User>();
        
        // avoid including the user in the results, but include the user if we still need more
        // results in the very end
        PersonMusicPlayView selfMusicPlayView = null;
        
        int totalPlays = 0;
        int numberOfFriendsWhoPlayedTrack = 0;
        List<?> history = q.getResultList();
        for (Object o : history) {
            TrackHistory h = (TrackHistory) o;

    		totalPlays = totalPlays + h.getTimesPlayed();

            User user = h.getUser();
            
            // we check this here to be able to short-circuit the rest of the logic if we have enough results
    		if (contacts.contains(user) && (!encounteredContacts.contains(user))) {
    			numberOfFriendsWhoPlayedTrack++;
    			encounteredContacts.add(user);
    		}
        	
    		if (views.size() >= MAX_RELATED_PEOPLE_RESULTS) {
                // the only reason we are still in this loop is to sum up the number of times this track
        		// was played and the number of friends who played it
        		continue;
        	}
    		

            // If the viewer is the user themself, we want to include
            // them in the result, because that prevents our pages
            // from looking strangely empty.
            if ((viewpoint != null) && viewpoint.isOfUser(user)) {  
                if (selfMusicPlayView == null) {   
            	    selfMusicPlayView = new PersonMusicPlayView(identitySpider.getPersonView(viewpoint, user), h.getLastUpdated());
                }
            	continue;
            }

            if (!contacts.isEmpty()) {
                // we are filling out the views with person's contacts
            	if (contacts.contains(user)) {          	
            		// it is correct to use the first track history that related to a given contact because we are interested
            		// in the most recent play and track histories were sorted by the time of the last update
            		if (views.get(user) == null) {
            			views.put(user, new PersonMusicPlayView(identitySpider.getPersonView(viewpoint, user), h.getLastUpdated()));
            		}
            	} else {
            		if (extraViews.get(user) == null) {    			
            			extraViews.put(user, new PersonMusicPlayView(identitySpider.getPersonView(viewpoint, user), h.getLastUpdated()));
              		}            		
            	}
            } else {
        		if (views.get(user) == null) {
        			views.put(user, new PersonMusicPlayView(identitySpider.getPersonView(viewpoint, user), h.getLastUpdated()));            			
        		}                	
            }
        }
        
        trackView.setTotalPlays(totalPlays);
        
        // setting the number of friends who played this track is only relevant if we have a user viewpoint
        if (viewpoint instanceof UserViewpoint) {
            trackView.setNumberOfFriendsWhoPlayedTrack(numberOfFriendsWhoPlayedTrack);
        }
        
        List<PersonMusicPlayView> personViewsForTrack = new ArrayList<PersonMusicPlayView>();
        personViewsForTrack.addAll(views.values());
        
        // the order of plays might not be chronological, but they will be in the order:
        // your friends, other people, you, and chronological within each category
        for (PersonMusicPlayView pmpv : extraViews.values()) {
            if	(personViewsForTrack.size() >= MAX_RELATED_PEOPLE_RESULTS) {
            	break;
            }
            personViewsForTrack.add(pmpv);
        }	
        
        if ((personViewsForTrack.size() < MAX_RELATED_PEOPLE_RESULTS) && (selfMusicPlayView != null)) {
        	personViewsForTrack.add(selfMusicPlayView);
        }
        
        trackView.setPersonMusicPlayViews(personViewsForTrack);

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
			// if you renable, take care of the fact that IN () isn't allowed if contacts is empty
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
	
	public List<NowPlayingTheme> getExampleNowPlayingThemes(Viewpoint viewpoint, int maxResults) {
		// FIXME pick certain good ones or something
		// FIXME EJBQL syntax is wrong, but don't have docs handy, will fix with the others
		// FIXME ascending order for now ensures our good default themes are visible, but prevents 
		// new good themes from ever showing up - need some kind of rating system or something else dynamic
		Query q = em.createQuery("FROM NowPlayingTheme t WHERE t.draft=0 ORDER BY t.creationDate ASC");
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

	public TrackSearchResult searchTracks(Viewpoint viewpoint, String queryString) {
		final String[] fields = { "Artist", "Album", "Name" };
		QueryParser queryParser = new MultiFieldQueryParser(fields, TrackIndexer.getInstance().createAnalyzer());
		queryParser.setDefaultOperator(Operator.AND);
		org.apache.lucene.search.Query query;
		try {
			query = queryParser.parse(queryString);
			
			Hits hits = TrackIndexer.getInstance().getSearcher().search(query);
			
			return new TrackSearchResult(hits);
			
		} catch (org.apache.lucene.queryParser.ParseException e) {
			return new TrackSearchResult("Can't parse query '" + queryString + "'");
		} catch (IOException e) {
			return new TrackSearchResult("System error while searching, please try again");
		}
	}
	
	public List<TrackView> getTrackSearchTracks(Viewpoint viewpoint, TrackSearchResult searchResult, int start, int count) {
		// The efficiency gain of having this wrapper is that we pass the real 
		// object to the method rather than the proxy; getTracks() can make many, 
		// many calls back against the MusicSystem.
		return searchResult.getTracks(this, viewpoint, start, count);
	}
		
	public void indexTracks(IndexWriter writer, DocumentBuilder<Track> builder, List<Object> ids) throws IOException {
		for (Object o : ids) {
			Track track = em.find(Track.class, o);
			if (track != null) {
				Document document = builder.getDocument(track, track.getId());
				writer.addDocument(document);
				logger.debug("Indexed track with id {}", o);
			} else {
				logger.debug("Couldn't find track to index");
			}
		}
		
	}
	
	public void indexAllTracks(IndexWriter writer, DocumentBuilder<Track> builder) throws IOException {
		List<?> l = em.createQuery("SELECT t FROM Track t").getResultList();
		List<Track> tracks = TypeUtils.castList(Track.class, l);
		
		for (Track track : tracks) {
			Document document = builder.getDocument(track, track.getId());
			writer.addDocument(document);
		}
	}

	public NowPlayingTheme getCurrentTheme(Viewpoint viewpoint, User user) {
		NowPlayingTheme current;
		try {
			current = getCurrentNowPlayingTheme(user);
		} catch (NotFoundException e) {
			current = null;
		}		
		return current;
	}
	
	private String buildGetFriendsThemesQuery(Viewpoint viewpoint, User user, Set<Contact> friends, boolean forCount) {
		String draftClause;
		if (viewpoint instanceof SystemViewpoint)
			draftClause = null;
		else if (viewpoint instanceof UserViewpoint)
			draftClause = "(t.draft=0 OR t.creator=:viewer)";
		else
			draftClause = "(t.draft=0)";

		StringBuilder sb = new StringBuilder("SELECT ");
		
		if (forCount) {
			sb.append("count(t)");
		} else {
			sb.append("t");
		}
		sb.append(" FROM NowPlayingTheme t ");
		/*
		 * FIXME convert to a join - this will break when a user has more than
		 * 200 contacts or so
		 */
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
		if (sb.charAt(sb.length() - 1) == ',') {
			sb.setLength(sb.length() - 1);
		}
		sb.append(")");

		if (draftClause != null) {
			sb.append("AND ");
			sb.append(draftClause);
		}
		sb.append(" ORDER BY creationDate DESC");
		return sb.toString();
	}

	public void getFriendsThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable) {
		// this will return no friends if we can't see this person's contacts
		// from our viewpoint
		Set<Contact> friends = identitySpider.getRawContacts(viewpoint, user);
		
		if (friends.isEmpty()) {
			pageable.setResults(new ArrayList<NowPlayingTheme>());
			pageable.setTotalCount(0);
			return;
		}
		
		Query q = em.createQuery(buildGetFriendsThemesQuery(viewpoint, user, friends, false));
		if (viewpoint instanceof UserViewpoint) {			
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());
		}
			
		q.setMaxResults(pageable.getCount());
		q.setFirstResult(pageable.getStart());
		pageable.setResults(TypeUtils.castList(NowPlayingTheme.class, q.getResultList()));
		q = em.createQuery(buildGetFriendsThemesQuery(viewpoint, user, friends, true));
		if (viewpoint instanceof UserViewpoint) {			
			q.setParameter("viewer", ((UserViewpoint)viewpoint).getViewer());
		}		
		pageable.setTotalCount(((Number) q.getSingleResult()).intValue());			
	}

	public void getMyThemes(Viewpoint viewpoint, User user, Pageable<NowPlayingTheme> pageable) {
		// this query will include our draft themes
		assert(viewpoint.isOfUser(user));
		Query q = em.createQuery(buildGetThemesQuery(viewpoint, user, false));
		q.setParameter("creator", user);
		q.setMaxResults(pageable.getCount());
		q.setFirstResult(pageable.getStart());
		pageable.setResults(TypeUtils.castList(NowPlayingTheme.class, q.getResultList()));
		q = em.createQuery(buildGetThemesQuery(viewpoint, user, true));
		q.setParameter("creator", user);		
		pageable.setTotalCount(((Number) q.getSingleResult()).intValue());		
	}
	
	private String buildGetThemesQuery(Viewpoint viewpoint, User creator, boolean forCount) {
		StringBuilder sb = new StringBuilder("SELECT ");
		if (forCount)
			sb.append("count(t)");
		else
			sb.append("t");
		sb.append(" FROM NowPlayingTheme t WHERE t.draft=0 ");
		if (creator != null)
			sb.append(" AND t.creator=:creator ");
		sb.append(" ORDER BY creationDate DESC");
		return sb.toString();
	}

	public void getAllThemes(Viewpoint viewpoint, Pageable<NowPlayingTheme> pageable) {
		Query q = em.createQuery(buildGetThemesQuery(viewpoint, null, false));
		q.setMaxResults(pageable.getCount());
		q.setFirstResult(pageable.getStart());
		pageable.setResults(TypeUtils.castList(NowPlayingTheme.class, q.getResultList()));
		q = em.createQuery(buildGetThemesQuery(viewpoint, null, true));
		pageable.setTotalCount(((Number) q.getSingleResult()).intValue());
	}
	
	public void addFeedTrack(AccountFeed feed, TrackFeedEntry entry, int entryPosition) {
		User user = feed.getAccount().getOwner();
		
		Map<String,String> properties = new HashMap<String,String>();
		properties.put("type", ""+TrackType.NETWORK_STREAM);
		properties.put("name", entry.getTrack());
		properties.put("artist", entry.getArtist());
		properties.put("album", entry.getAlbum());
		properties.put("url", entry.getPlayHref());
		
		Integer duration = Integer.parseInt(entry.getDuration());
		duration = duration/1000;  // Rhapsody duration info is in milliseconds, elswhere it's seconds
		
		properties.put("duration", ""+duration);
		
		//properties.put("format", "");            // could set to 'Rhapsody' or something like that
		//properties.put("fileSize", "");          // streaming tracks don't have a file size
		//properties.put("trackNumber", "");       // could parse this out of the PlayHref()
		//properties.put("discIdentifier", "");    // could use RCID information?
		
		/**
		 * Note that the track play date is set to the moment when the feed is processed.
		 * Although Rhapsody feeds have a date field, it seems to correspond to the date
		 * the song was published, not the date it was played by the user.
		 * 
		 * So instead, we create virtual play times based on the order of items in the 
		 * rhapsody feed.  The most recently played item is first; we use the current
		 * system time at feed parsing for that one.  The second most recently played item
		 * is second, we use the current system time minus one minute for that one.  And so on.
		 */
		
		long now = System.currentTimeMillis();
		long virtualPlayTime = now - (1000 * 60 * entryPosition);
		
		Track track = getTrack(properties);
		addTrackHistory(user, track, new Date(virtualPlayTime));
	}
	
	
	/**
	 * Try to find a Rhapsody friendly URL for this song.
	 * 
	 * @param songId
	 * @param artistName
	 * @param albumName
	 * @param trackNumber
	 * @return a working download url result, or null otherwise
	 */
	private String getRhapsodyDownloadUrl(String artistName, String albumName, int trackNumber) {
		if ((trackNumber>0) && (artistName != null) && (albumName != null)) {
			// Try to concoct a Rhapsody friendly URL; see:
			//  http://rws-blog.rhapsody.com/rhapsody_web_services/2006/04/new_album_urls.html
			String rhaplink = "http://play.rhapsody.com/" + rhapString(artistName) + "/" + rhapString(albumName) + "/track-" + trackNumber;
			
			boolean rhapLinkActive = false;
			RhapLink oldRhapLink = null;
			
			try {
				Query q = em.createQuery("FROM RhapLink rhaplink WHERE rhaplink.url = :rhaplink");
				q.setParameter("rhaplink", rhaplink);
				oldRhapLink = (RhapLink)(q.getSingleResult());
			} catch (EntityNotFoundException e) {
				//logger.debug("No cached rhaplink status for {}", rhaplink);
			}
			
			long now = System.currentTimeMillis();
			if ((oldRhapLink == null) || ((oldRhapLink.getLastUpdated() + RHAPLINK_EXPIRATION_TIMEOUT) < now)) {
				logger.debug("Unknown or outdated Rhapsody link; testing status for rhaplink {}", rhaplink);
				
				try {
					URL u = new URL(rhaplink);
					URLConnection connection = u.openConnection();
					if (!(connection instanceof HttpURLConnection)) {
						logger.warn("Got a weird connection of type {} for URL {}; skipping", connection.getClass().getName(), u);
					} else {
						HttpURLConnection httpConnection = (HttpURLConnection)connection;
						httpConnection.setRequestMethod("HEAD");
						httpConnection.setInstanceFollowRedirects(false);
						httpConnection.setConnectTimeout(REQUEST_TIMEOUT);
						httpConnection.setReadTimeout(REQUEST_TIMEOUT);
						httpConnection.setAllowUserInteraction(false);
						httpConnection.connect();
						
						// if we get a 200 response it's a playable URL
						// if we get anything else, it's not
						
						int httpResponse = httpConnection.getResponseCode();
						httpConnection.disconnect();
						
						logger.debug("response for rhaplink {} was {}", rhaplink, httpResponse);
						
						if (httpResponse == HttpURLConnection.HTTP_OK) {
							logger.debug("got working rhaplink {}", rhaplink);
							rhapLinkActive = true;
						}
					}
				} catch (MalformedURLException mue) {
					logger.warn("malformed URL when trying to fetch rhapsody link: " + mue.getMessage(), mue);
				} catch (IOException ioe) {
					logger.warn("IO exception when trying to fetch rhapsody link: " + ioe.getMessage(), ioe);
				}
				
				// remove the old rhaplink entry, if any, and store a new one
				try {
					if (oldRhapLink != null) {
						em.remove(oldRhapLink);
					}
					
					RhapLink newRhapLink = new RhapLink();
					newRhapLink.setUrl(rhaplink);
					newRhapLink.setActive(rhapLinkActive);
					newRhapLink.setLastUpdated(now);
					em.persist(newRhapLink);
				} catch (Exception e) {
					logger.error("Error updating RhapLink", e);
				}
				
			} else {
				// we found a rhaplink result that wasn't outdated, so return it
				rhapLinkActive = oldRhapLink.isActive();
				//logger.debug("cached rhaplink status for {} was {}", rhaplink, rhapLinkActive);
			}
		
			if (rhapLinkActive) {
				return rhaplink;
			} else {
				return null;
			}
		}
		return null;
	}
	
	/*
	 * Mangle a string to work in a Rhapsody friendly URL - remove all 
	 * punctuation and whitespace and lowercase it
	 */
	private static String rhapString(String s) {
		// strip all non-word characters, e.g. other than [a-zA-Z0-9]
		// TODO: figure out how to deal with broader character set, if Rhapsody even supports that?
		return s.replaceAll("[^a-zA-Z0-9]","").toLowerCase();
	}
}
