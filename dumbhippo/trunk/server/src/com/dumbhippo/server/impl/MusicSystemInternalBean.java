package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YahooSearchWebServices;

@Stateless
public class MusicSystemInternalBean implements MusicSystemInternal {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicSystemInternalBean.class);
	
	// 2 days
	static private final int EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 2;
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
	
	private ExecutorService threadPool;
	
	@PostConstruct
	public void init() {
		threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("MusicSystemBean");
				return t;
			}
		});
	}
	
	public Track getTrack(Map<String, String> properties) {
		
		final Track key = new Track(properties);
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<Track>() {
				
				public Track call() throws Exception {
					Query q;
					
					q = em.createQuery("from Track t where t.digest = :digest");
					q.setParameter("digest", key.getDigest());
					
					Track res;
					try {
						res = (Track) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						res = key;
						em.persist(res);
						
						// this is a fresh new track, so asynchronously fill in the Yahoo! search results
						hintNeedsYahooResults(res);
					}
					
					return res;	
				}			
			});
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
	
	private TrackHistory getCurrentTrack(Viewpoint viewpoint, User user) throws NotFoundException {
		List<TrackHistory> list = getTrackHistory(viewpoint, user, History.LATEST, 1);
		assert !list.isEmpty(); // supposed to throw NotFoundException if empty
		return list.get(0);
	}

	enum History {
		LATEST,
		FREQUENT
	}
	
	private List<TrackHistory> getTrackHistory(Viewpoint viewpoint, User user, History type, int maxResults) throws NotFoundException {
		logger.debug("getTrackHistory() type " + type + " for " + user + " max results " + maxResults);
		
		if (!identitySpider.isViewerFriendOf(viewpoint, user)) {
			logger.debug("Not allowed to see track history");
			throw new NotFoundException("Not allowed to see this user's track history");
		}

		Query q;
		
		String order = null;
		switch (type) {
		case LATEST:
			order = " ORDER BY h.lastUpdated DESC ";
			break;
		case FREQUENT:
			order = " ORDER BY h.timesPlayed DESC ";
			break;
		}
		
		q = em.createQuery("FROM TrackHistory h WHERE h.user = :user " + 
				order);
		q.setParameter("user", user);
		q.setMaxResults(maxResults);
		
		List<TrackHistory> results = new ArrayList<TrackHistory>();
 		try {
			List<?> rawResults = q.getResultList();
			for (Object o : rawResults) {
				TrackHistory h = (TrackHistory) o;
				if (h.getTrack() != null) // force-loads the track if it wasn't
					results.add(h);
				else
					logger.debug("Ignoring TrackHistory with null track");
			}
		} catch (EntityNotFoundException e) {
			logger.debug("No track history found in query");
		}

		if (results.isEmpty()) {
			logger.debug("No track history results");
			throw new NotFoundException("User has no track history");
		} else {
			return results;
		}
	}

	private List<TrackHistory> getTrackHistory(Viewpoint viewpoint, Group group, History type, int maxResults) throws NotFoundException {
		logger.debug("getTrackHistory() type " + type + " for " + group + " max results " + maxResults);

		// This is not very efficient now is it...
		
		Set<User> members = groupSystem.getUserMembers(viewpoint, group, MembershipStatus.ACTIVE);
		List<TrackHistory> results = new ArrayList<TrackHistory>();
		for (User m : members) {
			try {
				List<TrackHistory> memberHistory = getTrackHistory(viewpoint, m, type, maxResults);
				results.addAll(memberHistory);
			} catch (NotFoundException e) {
				// ignore
			}
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
		
		if (results.isEmpty()) {
			logger.debug("No track history results");
			throw new NotFoundException("Group has no track history");
		} else {
			return results;
		}
	}
	
	static private class YahooSongTask implements Callable<List<YahooSongResult>> {

		private Track track;
		
		public YahooSongTask(Track track) {
			this.track = track;
		}
		
		public List<YahooSongResult> call() {
			logger.debug("Running YahooSongTask thread");
			
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getYahooSongResultsSync(track);
		}
	}

	static private class YahooSongDownloadTask implements Callable<List<YahooSongDownloadResult>> {

		private String songId;
		
		public YahooSongDownloadTask(String songId) {
			this.songId = songId;
		}
		
		public List<YahooSongDownloadResult> call() {
			logger.debug("Running YahooSongDownloadTask thread");
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getYahooSongDownloadResultsSync(songId);
		}
	}

	static private class GetTrackViewTask implements Callable<TrackView> {

		private Track track;
		
		public GetTrackViewTask(Track track) {
			this.track = track;
		}
		
		public TrackView call() {
			logger.debug("Running GetTrackViewTask thread");
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);
			
			return musicSystem.getTrackView(track);
		}
	}
	
	public void hintNeedsYahooResults(Track track) {
		// called for side effect to kick off querying the results
		getTrackViewAsync(track);
	}
	
	private List<YahooSongResult> updateSongResultsSync(List<YahooSongResult> oldResults, Track track) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
		List<YahooSongResult> newResults = ws.lookupSong(track.getArtist(), track.getAlbum(), track.getName(),
				track.getDuration(), track.getTrackNumber());
		// Match new results to old results with same song id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times so we don't re-query too often.
		if (newResults.isEmpty()) {
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			for (YahooSongResult old : oldResults) {
				old.setLastUpdated(new Date(updateTime));
			}
			return oldResults;
		}
		
		List<YahooSongResult> results = new ArrayList<YahooSongResult>();
		for (YahooSongResult old : oldResults) {
			boolean stillFound = false;
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
			n.setTrack(track);
			em.persist(n);
			results.add(n);
		}
		
		return results;
	}
	
	public List<YahooSongResult> getYahooSongResultsSync(Track track) {
		
		// yahoo lookup requires these fields, so just bail without them
		if (track.getArtist() == null ||
				track.getAlbum() == null ||
				track.getName() == null) {
			logger.debug("Track " + track + " missing artist album or name, can't get yahoo stuff");
			return Collections.emptyList();
		}
		
		Query q;
		
		q = em.createQuery("from YahooSongResult song where song.track = :track");
		q.setParameter("track", track);
		
		List<YahooSongResult> results;
		try {
			List<?> objects = q.getResultList();
			results = new ArrayList<YahooSongResult>(); 
			for (Object o : objects) {
				results.add((YahooSongResult) o);
			}
		} catch (EntityNotFoundException e) {
			results = Collections.emptyList();
		}
		
		boolean needNewQuery = results.isEmpty();
		if (!needNewQuery) {
			long now = System.currentTimeMillis();
			for (YahooSongResult r : results) {
				if ((r.getLastUpdated().getTime() + EXPIRATION_TIMEOUT) < now) {
					needNewQuery = true;
					logger.debug("Outdated Yahoo result, will need to renew the search");
					break;
				}
			}
		} else {
			logger.debug("No Yahoo results for track, will need to search for them");
		}
		
		if (needNewQuery) {
			return updateSongResultsSync(results, track);
		} else {
			logger.debug("Returning Yahoo song results from database cache");
			return results;
		}
	}

	private List<YahooSongDownloadResult> updateSongDownloadResultsSync(List<YahooSongDownloadResult> oldResults, String songId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT);
		List<YahooSongDownloadResult> newResults = ws.lookupDownloads(songId);
		// Match new results to old results with same song id, updating the old row.
		// For new rows, create the row.
		// For rows not in the new yahoo return, drop the row.
		// If the new search returned nothing, just change the updated 
		// times so we don't re-query too often.
		if (newResults.isEmpty()) {
			// ensure we won't update for FAILED_QUERY_TIMEOUT
			long updateTime = System.currentTimeMillis() - EXPIRATION_TIMEOUT + FAILED_QUERY_TIMEOUT;
			for (YahooSongDownloadResult old : oldResults) {
				old.setLastUpdated(new Date(updateTime));
			}
			return oldResults;
		}
		
		List<YahooSongDownloadResult> results = new ArrayList<YahooSongDownloadResult>();
		for (YahooSongDownloadResult old : oldResults) {
			boolean stillFound = false;
			for (YahooSongDownloadResult n : newResults) {
				if (n.getSource() == old.getSource()) {
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
		for (YahooSongDownloadResult n : newResults) {
			em.persist(n);
			results.add(n);
		}
		
		return results;
	}
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId) {
		Query q;
		
		q = em.createQuery("from YahooSongDownloadResult download where download.songId = :songId");
		q.setParameter("songId", songId);
		
		List<YahooSongDownloadResult> results;
		try {
			List<?> objects = q.getResultList();
			results = new ArrayList<YahooSongDownloadResult>(); 
			for (Object o : objects) {
				results.add((YahooSongDownloadResult) o);
			}
		} catch (EntityNotFoundException e) {
			results = Collections.emptyList();
		}
		
		boolean needNewQuery = results.isEmpty();
		if (!needNewQuery) {
			long now = System.currentTimeMillis();
			for (YahooSongDownloadResult r : results) {
				if ((r.getLastUpdated().getTime() + EXPIRATION_TIMEOUT) < now) {
					needNewQuery = true;
					logger.debug("Outdated Yahoo download result, will need to renew the search");
					break;
				}
			}
		} else {
			logger.debug("No Yahoo download results for song id, will need to search for them");
		}
		
		if (needNewQuery) {
			return updateSongDownloadResultsSync(results, songId);
		} else {
			logger.debug("Returning Yahoo song download results from database cache");
			return results;
		}
	}
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track) {
		FutureTask<List<YahooSongResult>> futureSong =
			new FutureTask<List<YahooSongResult>>(new YahooSongTask(track));
		threadPool.execute(futureSong);
		return futureSong;
	}
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId) {
		FutureTask<List<YahooSongDownloadResult>> futureDownload =
			new FutureTask<List<YahooSongDownloadResult>>(new YahooSongDownloadTask(songId));
		threadPool.execute(futureDownload);
		return futureDownload;		
	}
	
	public TrackView getTrackView(Track track) {
		TrackView view = new TrackView(track);
		
		// this should never throw due to Yahoo failure;
		// we should just return a view without the download 
		// sources.
		try {
			// get our song IDs; no point doing it async...
			List<YahooSongResult> songs = getYahooSongResultsSync(track);
			
			// start a thread to get each download url
			List<Future<List<YahooSongDownloadResult>>> downloads = new ArrayList<Future<List<YahooSongDownloadResult>>>(); 
			for (YahooSongResult song : songs) {
				downloads.add(getYahooSongDownloadResultsAsync(song.getSongId()));
			}
			
			for (Future<List<YahooSongDownloadResult>> futureDownloads : downloads) {
				List<YahooSongDownloadResult> ds;
				try {
					ds = futureDownloads.get();
				} catch (InterruptedException e) {
					logger.debug("Thread interrupted getting song download info from yahoo", e);
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					logger.debug("Exception getting song download info from yahoo", e);
					throw new RuntimeException(e);
				}
				for (YahooSongDownloadResult d : ds) {
					// if two search results are for the same source, first is assumed better
					if (view.getDownloadUrl(d.getSource()) == null) {
						view.setDownloadUrl(d.getSource(), d.getUrl());
						logger.debug("adding download url for " + d.getSource().getYahooSourceName());
					} else {
						logger.debug("ignoring second download url for " + d.getSource().getYahooSourceName());
					}
				}
			}
		} catch (Exception e) {
			logger.debug("Failed to get Yahoo! search information for TrackView " + view, e);
		}
		
		return view;
	}
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackView(current.getTrack());
	}
	
	public Future<TrackView> getTrackViewAsync(Track track) {
		FutureTask<TrackView> futureView =
			new FutureTask<TrackView>(new GetTrackViewTask(track));
		threadPool.execute(futureView);
		return futureView;				
	}
	
	public Future<TrackView> getCurrentTrackViewAsync(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackViewAsync(current.getTrack());
	}
	
	private List<TrackView> getViewsFromTracks(List<Track> tracks) {
		
		logger.debug("Getting TrackViews from tracks list with " + tracks.size() + " items");
		
		// spawn a bunch of yahoo updater threads in parallel
		List<Future<TrackView>> futureViews = new ArrayList<Future<TrackView>>(tracks.size());
		for (Track t : tracks) {
			futureViews.add(getTrackViewAsync(t));
		}
	
		// now harvest all the results
		List<TrackView> views = new ArrayList<TrackView>(tracks.size());
		for (Future<TrackView> fv : futureViews) {
			TrackView v;
			try {
				v = fv.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Future<TrackView> was interrupted", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Future<TrackView> had execution exception", e);
			}
			
			assert v != null;
			
			views.add(v);
		}
		
		return views;
	}
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException {
		logger.debug("getLatestTrackViews() for user " + user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, maxResults);
		
		List<Track> tracks = new ArrayList<Track>(history.size());
		for (TrackHistory h : history)
			tracks.add(h.getTrack());
		return getViewsFromTracks(tracks);
	}
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException {
		logger.debug("getFrequentTrackViews() for user " + user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.FREQUENT, maxResults);
		
		List<Track> tracks = new ArrayList<Track>(history.size());
		for (TrackHistory h : history)
			tracks.add(h.getTrack());
		return getViewsFromTracks(tracks);
	}

	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException {
		logger.debug("getLatestTrackViews() for group {}", group);
		List<TrackHistory> history = getTrackHistory(viewpoint, group, History.LATEST, maxResults);
		
		List<Track> tracks = new ArrayList<Track>(history.size());
		for (TrackHistory h : history)
			tracks.add(h.getTrack());
		return getViewsFromTracks(tracks);
	}

	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException {
		logger.debug("getFrequentTrackViews() for group {}", group);
		List<TrackHistory> history = getTrackHistory(viewpoint, group, History.FREQUENT, maxResults);
		
		List<Track> tracks = new ArrayList<Track>(history.size());
		for (TrackHistory h : history)
			tracks.add(h.getTrack());
		return getViewsFromTracks(tracks);
	}
}
