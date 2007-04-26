package com.dumbhippo.server.impl;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
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

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.UserChangedEvent;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackFeedEntry;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.TrackType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.search.SearchSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.TrackIndexer;
import com.dumbhippo.server.TrackSearchResult;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.PersonMusicPlayView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooArtistData;
import com.dumbhippo.services.YahooSongData;
import com.dumbhippo.services.YahooSongDownloadData;
import com.dumbhippo.services.caches.AlbumAndArtist;
import com.dumbhippo.services.caches.AmazonAlbumCache;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.RhapsodyDownloadCache;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.services.caches.YahooAlbumCache;
import com.dumbhippo.services.caches.YahooAlbumSongsCache;
import com.dumbhippo.services.caches.YahooArtistAlbumsCache;
import com.dumbhippo.services.caches.YahooArtistCache;
import com.dumbhippo.services.caches.YahooSongCache;
import com.dumbhippo.services.caches.YahooSongDownloadCache;

@Stateless
public class MusicSystemInternalBean implements MusicSystemInternal {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicSystemInternalBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private Configuration config;
	
	@EJB
	private SearchSystem searchSystem;
	
	@WebServiceCache
	private AmazonAlbumCache amazonAlbumCache;
	
	@WebServiceCache
	private RhapsodyDownloadCache rhapsodyDownloadCache;
	
	@WebServiceCache
	private YahooAlbumCache yahooAlbumCache;
	
	@WebServiceCache
	private YahooArtistCache yahooArtistCache;
	
	@WebServiceCache
	private YahooSongCache yahooSongCache;

	@WebServiceCache
	private YahooAlbumSongsCache yahooAlbumSongsCache;	
	
	@WebServiceCache
	private YahooSongDownloadCache yahooSongDownloadCache;
	
	@WebServiceCache
	private YahooArtistAlbumsCache yahooArtistAlbumsCache;
	
	@EJB
	private Notifier notifier;	
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	private static ExecutorService threadPool;
	private static boolean shutdown = false;
	
	protected synchronized static ExecutorService getThreadPool() {
		if (shutdown)
			throw new RuntimeException("getThreadPool() called after shutdown");
			
		if (threadPool == null) {
			threadPool = ThreadUtils.newCachedThreadPool("ws cache pool");
		}
		
		return threadPool;
	}
	
	public static void shutdown() {
		synchronized (MusicSystemInternalBean.class) {
			shutdown = true;
			
			if (threadPool != null) {
				ThreadUtils.shutdownAndAwaitTermination(threadPool);
				threadPool = null;
			}
		}	
	}	
	
	/**
	 * Gets the ID of a potentially newly-created track.
	 * Creates the track in another transaction, i.e. the caller
	 * can't use this id, but can pass it to a new transaction for 
	 * use.
	 * 
	 * Note that if the caller has previously looked up and modified
	 * the same track (acquiring a write lock), this will deadlock, 
	 * so use with great caution.
	 * 
	 * @param properties properties of the track
	 * @return the track id
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	private long getTrackIdIsolated(final Map<String, String> properties) {
		Track detached;
		try {
			detached = runner.runTaskInNewTransaction(new Callable<Track>() {
				public Track call() {
					return getTrack(properties);
				}
			});
		} catch (Exception e) {
			logger.error("Failed to create Track: {}", e.getMessage());
			ExceptionUtils.throwAsRuntimeException(e);
			// not reached
			throw new RuntimeException(e);
		}
		return detached.getId();
	}
	
	public Track getTrack(Map<String, String> properties) {
		
		final Track key = new Track(properties);
		try {
			Track track = runner.runTaskThrowingConstraintViolation(new Callable<Track>() {
				
				public Track call() throws Exception {
					Query q;
					
					q = em.createQuery("FROM Track t WHERE t.digest = :digest");
					q.setParameter("digest", key.getDigest());
					
					Track res;
					try {
						res = (Track) q.getSingleResult();
					} catch (NoResultException e) {
						res = key;
						em.persist(res);
						
						final long trackId = res.getId();
						// this is a fresh new track, so asynchronously fill in the Yahoo! search results
						// only after we commit the transaction so the Track is visible
						runner.runTaskOnTransactionCommit(new Runnable() {
							public void run() {
								hintNeedsRefresh(trackId);
							}
						});
					}
					
					return res;	
				}			
			});
			searchSystem.indexTrack(track);
			return track;
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			throw new RuntimeException(e); // not reached
		}
	}

	private void addTrackHistory(final User user, final Track track, final Date now, final boolean isNative) {
		runner.runTaskRetryingOnConstraintViolation(new Runnable() {				
			public void run() {
				User u = em.find(User.class, user.getId());					
				if (isNative) {
					u.getAccount().setNativeMusicSharingTimestamp(new Date());
				}
				// This can happen when we get an empty track notification to signal
				// playback stopped - we still want to update the native timestamp in that case
				if (track == null)
					return;
				
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
				} catch (NoResultException e) {
					res = new TrackHistory(user, track);
					res.setLastUpdated(now);
					res.setTimesPlayed(1);
					em.persist(res);
				}
			}
		});
		
		// update the stack with this new listen event
		if (track != null)
			notifier.onTrackPlayed(user, track, now);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public void setCurrentTrack(final User user, final Track track, final boolean isNative) {
		addTrackHistory(user, track, new Date(), isNative);
	}
	 
	// Although this is marked as SUPPORTS - you should never invoke this
	// method from any code holding a transaction that could have potentially
	// modified a Track
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public void setCurrentTrack(User user, Map<String,String> properties, final boolean isNative) {
		
		// empty properties means "not listening to any track" - we always
		// keep the latest track with content, we don't set CurrentTrack to null
		final long trackId;		
		if (properties.size() == 0)
			trackId = -1;
		else
			trackId = getTrackIdIsolated(properties);
		
		final String userId = user.getId();
		
		// trackId is invalid in this outer transaction, so we need a new one
		runner.runTaskInNewTransaction(new Runnable() {

			public void run() {
				User u = em.find(User.class, userId);
				if (u == null)
					throw new RuntimeException("database isolation problem (?): user id not found " + userId);
				Track t = null;				
				if (trackId != -1) {
					t = em.find(Track.class, trackId);
					if (t == null)
						throw new RuntimeException("database isolation problem (?): track id not found " + trackId);
				}
				setCurrentTrack(u, t, isNative);
			}
			
		});
	}
	
	// Although this is marked as SUPPORTS - you should never invoke this
	// method from any code holding a transaction that could have potentially
	// modified a Track	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public void addHistoricalTrack(User user, Map<String,String> properties, final boolean isNative) {
		// for now there's no difference here, but eventually we might have the 
		// client supply some properties like the date of listening instead of 
		// pretending we "just" listened to this track.
		setCurrentTrack(user, properties, isNative);
	}
	
	// Although this is marked as SUPPORTS - you should never invoke this
	// method from any code holding a transaction that could have potentially
	// modified a Track		
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public void addHistoricalTrack(final User user, final Map<String,String> properties, final long listenDate, final boolean isNative) {
		final long trackId = getTrackIdIsolated(properties);
		
		final String userId = user.getId();
		// trackId is invalid in this outer transaction, so we need a new one
		runner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				Track t = em.find(Track.class, trackId);
				if (t == null)
					throw new RuntimeException("database isolation problem (?): track id not found " + trackId);
				User u = em.find(User.class, userId);
				if (u == null)
					throw new RuntimeException("database isolation problem (?): user id not found " + userId);
				addTrackHistory(user, t, new Date(listenDate), isNative);
			}
		});
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public void queueMusicChange(final Guid userId) {
		// LiveState.queueUpdate() expects to be called in a transaction, and we
		// don't have one since the music system code needs to be called not
		// in a transaction.
		runner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				LiveState.getInstance().queueUpdate(new UserChangedEvent(userId, UserChangedEvent.Detail.MUSIC)); 
			}
		});
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
	
	private int queryCount(User user, boolean limitToOne) {
		Query q;
		
		// not sure setMaxResults will work with COUNT(*) so this is a little annoying
		if (limitToOne) {
			q = em.createQuery("SELECT h.id FROM TrackHistory h WHERE h.user = :user");
			q.setMaxResults(1);
		} else {
			q = em.createQuery("SELECT COUNT(*) FROM TrackHistory h WHERE h.user = :user");
		}
		q.setParameter("user", user);
		
		if (limitToOne) {
			try {
				q.getSingleResult();
				return 1;
			} catch (NoResultException e) {
				return 0;
			}
		} else {
			Object o = q.getSingleResult();
			return ((Number)o).intValue();	
		}
	}
	
	public boolean hasTrackHistory(Viewpoint viewpoint, User user) {
		if (!identitySpider.getMusicSharingEnabled(user, Enabled.AND_ACCOUNT_IS_ACTIVE)) {
			return false;
		}
		return queryCount(user, true) > 0;
	}
	
	static private class GetTrackViewTask implements Callable<TrackView> {

		private Track track;
		private TrackHistory trackHistory;
		
		public GetTrackViewTask(Track track, TrackHistory trackHistory) {
			this.track = track;
			this.trackHistory = trackHistory;
		}
		
		public TrackView call() {
			logger.debug("Entering GetTrackViewTask thread for track {}", track);
			// we do this instead of an inner class to work right with threads
			MusicSystemInternal musicSystem = EJBUtil.defaultLookup(MusicSystemInternal.class);

			logger.debug("Obtained transaction in GetTrackViewTask thread for track {}", track);
			
			if (trackHistory != null)
				return musicSystem.getTrackView(trackHistory);
			else
				return musicSystem.getTrackView(track);				
		}
	}

	static private void hintNeedsRefresh(final long trackId) {
		// this has to be in another thread since it's called from 
		// an after-commit handler and looking up session beans doesn't 
		// really work in that handler apparently
		getThreadPool().execute(new Runnable() {
			public void run() {
				EJBUtil.defaultLookup(MusicSystemInternal.class).getTrackViewAsync(trackId);
			}
		});
	}
	
	
	public int parseReleaseYear(String releaseDate) {
		if (releaseDate == null)
			return -1;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd");
		try {
		    Date date = dateFormat.parse(releaseDate);
		    Calendar calendar = new GregorianCalendar();
		    calendar.setTime(date);
		    return calendar.get(Calendar.YEAR);
		} catch (ParseException e) {
			return -1;
		}
	}
	
	private void fillAlbumInfo(YahooAlbumData yahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum, AlbumView albumView) {			
		// Note that if neither Amazon nor Yahoo! has a small image url, we want to leave 
		// it null so AlbumView can default to our "no image" picture. This also 
		// means that albumView.getSmallImageUrl() never returns null
		
		// first see what we can get from yahoo album result
		if (yahooAlbum != null) {
		    albumView.setReleaseYear(parseReleaseYear(yahooAlbum.getReleaseDate()));
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
		AmazonAlbumData amazonAlbum = futureAmazonAlbum == null ? null : ThreadUtils.getFutureResultNullOnException(futureAmazonAlbum);
					
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
	}
	
	private void fillAlbumInfo(Future<YahooAlbumData> futureYahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum, AlbumView albumView) {
		YahooAlbumData yahooAlbum = null;
	    if (futureYahooAlbum != null) {
	    	//it is ok to call fillAlbumInfo below with yahooAlbum being null
			yahooAlbum = ThreadUtils.getFutureResultNullOnException(futureYahooAlbum);
	    }
		fillAlbumInfo(yahooAlbum, futureAmazonAlbum, albumView);				
	}
	
	private static class RhapsodyFutureDownload {
		private String url;
		private Future<Boolean> future;
		private TrackView trackView;
		
		RhapsodyFutureDownload(String url, Future<Boolean> future, TrackView trackView) {
			this.url = url;
			this.future = future;
			this.trackView = trackView;
		}
		
		public void storeInTrackView() {
			// If we already found one download URL, don't replace it
			if (trackView.getDownloadUrl(SongDownloadSource.RHAPSODY) != null)
				return;
			
			Boolean haveUrl;
			try {
				haveUrl = future.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
			if (haveUrl != null && haveUrl)
				trackView.setDownloadUrl(SongDownloadSource.RHAPSODY, url);
		}
	}
	
	private void fillAlbumTracks(Viewpoint viewpoint, YahooSongData yahooSong, Future<List<? extends YahooSongData>> futureAlbumTracks, AlbumView albumView) {
			TreeMap<Integer, TrackView> sortedTracks = new TreeMap<Integer, TrackView>();
			TreeMap<Integer, List<Future<List<? extends YahooSongDownloadData>>>> trackDownloads = 
				new TreeMap<Integer, List<Future<List<? extends YahooSongDownloadData>>>>();
			List<? extends YahooSongData> albumTracks = ThreadUtils.getFutureResultEmptyListOnException(futureAlbumTracks, YahooSongData.class);
			// now we have a fun task of sorting these YahooSongData by track number and filtering out
			// the ones with -1 or 0 track number (YahooSongData has -1 track number by default, 
			// and 0 track number if it was returned set to that by the web service, which is typical
			// for songs for which track number is inapplicable or unknown)				
			// TODO: we might consider not storing songs with -1 or 0 track number in the database
			// when we are doing updateYahooSongDatas based on an albumId, yet we might still
			// have some songs with -1 or 0 track number because of getting yahoo songs based on
			// tracks that were played (note: for Track objects -1 indicates that the track number
			// is inapplicable or unknown and 0 is not a valid value)
			// TODO: this can also be part of a threaded task
			for (YahooSongData albumTrack : albumTracks) {
				// so fun! now we get to pair up the multiple YahooSongData into
				// a single track, and most importantly, get the download urls
				// based on each YahooSongData and add them to the right albumTrack
				
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
                        // because a single track can have multiple valid YahooSongDatas associated
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
                    	ArrayList<Future<List<? extends YahooSongDownloadData>>> downloads =
                    		new ArrayList<Future<List<? extends YahooSongDownloadData>>>();
                    	// if the trackNumber was positive this can't be a no results marker,
                    	// so the song id should also not be null
                    	downloads.add(yahooSongDownloadCache.getAsync(albumTrack.getSongId()));
                    	trackDownloads.put(albumTrack.getTrackNumber(), downloads);
                    } else {
                    	trackDownloads.get(albumTrack.getTrackNumber()).add(yahooSongDownloadCache.getAsync(albumTrack.getSongId()));
                    }
				}
			}
			
			List<RhapsodyFutureDownload> rhapsodyFutures = new ArrayList<RhapsodyFutureDownload>();
			
    		for (Integer trackNumber : sortedTracks.keySet()) {
    			TrackView trackView = sortedTracks.get(trackNumber);       			
    			for (Future<List<? extends YahooSongDownloadData>> futureDownloads : trackDownloads.get(trackNumber)) {
					List<? extends YahooSongDownloadData> ds = ThreadUtils.getFutureResultEmptyListOnException(futureDownloads, YahooSongDownloadData.class);					        			
    			    for (YahooSongDownloadData d : ds) {
					    // if two search results are for the same source, first is assumed better
					    if (trackView.getDownloadUrl(d.getSource()) == null) {
						    trackView.setDownloadUrl(d.getSource(), d.getUrl());
						    //logger.debug("adding download url for {}", d.getSource().getYahooSourceName());
					    } else {
						    logger.debug("ignoring second download url for {}", d.getSource().getYahooSourceName());
					    }
    			    }
				}
    			for (String rhapsodyDownloadUrl : rhapsodyDownloadCache.buildLinks(trackView.getAlbum(), trackView.getArtist(), trackView.getName()))
					rhapsodyFutures.add(new RhapsodyFutureDownload(rhapsodyDownloadUrl, 
							rhapsodyDownloadCache.getAsync(rhapsodyDownloadUrl),
							trackView));
			}
			
    		for (RhapsodyFutureDownload rhapsodyFuture : rhapsodyFutures) {
    			rhapsodyFuture.storeInTrackView();
    		}

			for (TrackView trackView : sortedTracks.values()) {
				albumView.addTrack(trackView);
			}
	}
	
	public TrackHistory lookupTrackHistory(Guid trackHistoryId) throws NotFoundException {
		TrackHistory trackHistory = em.find(TrackHistory.class, trackHistoryId.toString());
		if (trackHistory == null)
			throw new NotFoundException("No such TrackHistory");
	
		return trackHistory;
	}
	
	public TrackView getTrackView(Track track) {
		return getTrackView(track, null);
	}
	
	public TrackView getTrackView(TrackHistory trackHistory) {
		return getTrackView(trackHistory.getTrack(), trackHistory);
	}
	
	private TrackView getTrackView(Track track, TrackHistory trackHistory) {

		TrackView view = new TrackView(track, trackHistory);

		// this method should never throw due to Yahoo or Amazon failure;
		// we should just return a view without the extra information.
		
		YahooSongData yahooSong = null;
		Future<YahooAlbumData> futureYahooAlbum = null;
		Future<AmazonAlbumData> futureAmazonAlbum;
		if (track.getAlbum() != null && track.getArtist() != null)
			futureAmazonAlbum = amazonAlbumCache.getAsync(new AlbumAndArtist(track.getAlbum(), track.getArtist()));
		else
			futureAmazonAlbum = null;

		List<RhapsodyFutureDownload> rhapsodyFutures = new ArrayList<RhapsodyFutureDownload>();
		
		for (String rhapsodyDownloadUrl : rhapsodyDownloadCache.buildLinks(track.getAlbum(), track.getArtist(), track.getName()))
			rhapsodyFutures.add(new RhapsodyFutureDownload(rhapsodyDownloadUrl, 
					rhapsodyDownloadCache.getAsync(rhapsodyDownloadUrl),
					view));

		// get our song IDs; no point doing it async...
		List<? extends YahooSongData> songs = yahooSongCache.getSync(track);
		
		// start a thread to get each download url
		List<Future<List<? extends YahooSongDownloadData>>> downloads = new ArrayList<Future<List<? extends YahooSongDownloadData>>>(); 
		for (YahooSongData song : songs) {
			downloads.add(yahooSongDownloadCache.getAsync(song.getSongId()));
			// we need one of these song results to get an album info for the track
			if (yahooSong == null) {
			 	yahooSong = song;
			}
		}
		// if yahooSong is not null, get a YahooAlbumData for it, so we can get an album artwork for the track
		if (yahooSong != null) {
			futureYahooAlbum = yahooAlbumCache.getAsync(yahooSong.getAlbumId());
		}
		
		for (Future<List<? extends YahooSongDownloadData>> futureDownloads : downloads) {
			List<? extends YahooSongDownloadData> ds = ThreadUtils.getFutureResultEmptyListOnException(futureDownloads, YahooSongDownloadData.class);

			for (YahooSongDownloadData d : ds) {
				// if two search results are for the same source, first is assumed better
				if (view.getDownloadUrl(d.getSource()) == null) {
					view.setDownloadUrl(d.getSource(), d.getUrl());
					//logger.debug("adding download url for {}", d.getSource().getYahooSourceName());
				} else {
					logger.debug("ignoring second download url for {}", d.getSource().getYahooSourceName());
				}
			}
		}
		
		for (RhapsodyFutureDownload rhapsodyFuture : rhapsodyFutures)
			rhapsodyFuture.storeInTrackView();

		fillAlbumInfo(futureYahooAlbum, futureAmazonAlbum, view.getAlbumView());
		
		return view;
	}

	public AlbumView getAlbumView(Viewpoint viewpoint, YahooSongData yahooSong, YahooAlbumData yahooAlbum, Future<AmazonAlbumData> futureAmazonAlbum, Future<List<? extends YahooSongData>> futureAlbumTracks) {
		AlbumView view = new AlbumView();
		fillAlbumTracks(viewpoint, yahooSong, futureAlbumTracks, view);
		fillAlbumInfo(yahooAlbum, futureAmazonAlbum, view);

		return view;		
	}
	
	private void fillArtistInfo(YahooArtistData yahooArtist, ExpandedArtistView artistView) {		
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
	 
	private void fillAlbumsByArtist(Viewpoint viewpoint, YahooArtistData yahooArtist, Pageable<AlbumView> albumsByArtist, 
			                        YahooAlbumData albumToExclude, ExpandedArtistView view) {
		// get albums using an artistId
		List<? extends YahooAlbumData> albums = yahooArtistAlbumsCache.getSync(yahooArtist.getArtistId());

		// the artist has this many total albums, but (maybe?) our pageable
		// should not include the "excluded album" ... so we set the total count
		// on it below the exclusion code and the artist count above
		view.setTotalAlbumsByArtist(albums.size());
		
		if (albumToExclude != null) {
			Iterator<? extends YahooAlbumData> i = albums.iterator();
			while (i.hasNext()) {
				YahooAlbumData album = i.next();
				if (album.getAlbumId().equals(albumToExclude.getAlbumId())) {
					i.remove();
					break;
				}	
			}
		}
		
		// sort the list so we get predictable paging.
		// this would be much nicer if all the compilation albums with only 
		// 1 or 2 tracks from the artist sorted later. However, at this point
		// the YahooAlbumData does not have enough info to decide which things
		// are compilations - the artistIds are all the same, and there's no 
		// track counts.
		Collections.sort(albums, new Comparator<YahooAlbumData>() {
			public int compare(YahooAlbumData first, YahooAlbumData second) {
				return String.CASE_INSENSITIVE_ORDER.compare(first.getAlbum(), second.getAlbum());
			}
		});
		
		// whack the results we don't need so we don't bother converting them to AlbumView
		albumsByArtist.chopForPageResults(albums);
		
		fillArtistInfo(yahooArtist, view);
		
		// can start threads to get each album cover and to get songs for each album in parallel 
		Map<String, Future<AmazonAlbumData>> futureAmazonAlbums = new HashMap<String, Future<AmazonAlbumData>>();		
		Map<String, Future<List<? extends YahooSongData>>> futureTracks = new HashMap<String, Future<List<? extends YahooSongData>>>();
		Map<String, YahooAlbumData> yahooAlbums = new HashMap<String, YahooAlbumData>();
		for (YahooAlbumData yahooAlbum : albums) {
			if (yahooAlbum.getAlbum() != null && yahooAlbum.getArtist() != null) {
				Future<AmazonAlbumData> futureAmazonAlbum = amazonAlbumCache.getAsync(new AlbumAndArtist(yahooAlbum.getAlbum(), yahooAlbum.getArtist()));
				futureAmazonAlbums.put(yahooAlbum.getAlbumId(), futureAmazonAlbum);
			}
			// getAlbumTracksAsync is responsible for doing all the sorting, so we get a clean sorted list here
			futureTracks.put(yahooAlbum.getAlbumId(), yahooAlbumSongsCache.getAsync(yahooAlbum.getAlbumId()));
			yahooAlbums.put(yahooAlbum.getAlbumId(), yahooAlbum);
		}
		
		// all key sets should be the same
		for (String albumId : yahooAlbums.keySet()) {
			YahooAlbumData yahooAlbum = yahooAlbums.get(albumId);
			Future<AmazonAlbumData> futureAmazonAlbum = futureAmazonAlbums.get(albumId);
			Future<List<? extends YahooSongData>> futureAlbumTracks = futureTracks.get(albumId);
			AlbumView albumView = getAlbumView(viewpoint, null, yahooAlbum, futureAmazonAlbum, futureAlbumTracks);
			view.addAlbum(albumView);
		}	
	}
	
	private ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, String artist, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		ExpandedArtistView view = new ExpandedArtistView(artist);

	    // this call would throw a NotFoundException if an artist was not found, and would never return null
        YahooArtistData yahooArtist = yahooArtistCache.getSyncByName(artist);				

		if (yahooArtist == null) {
			 throw new NotFoundException("Artist " + artist + " was not found.");
		}
		
        fillAlbumsByArtist(viewpoint, yahooArtist, albumsByArtist, null, view);
        
		return view;
	}
	
	public ExpandedArtistView getExpandedArtistView(Viewpoint viewpoint, YahooSongData song, YahooAlbumData album, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		
		// we require the album field
		if (album == null) {
			logger.error("album is null when requesting an expanded artist view with an album");
			throw new RuntimeException("album is null when requesting an expanded artist view with an album");
		}
		
		ExpandedArtistView view = new ExpandedArtistView(album.getArtist());		

		// FIXME pass this in from the caller, don't query it again ?
        YahooArtistData yahooArtist = yahooArtistCache.getSync(album.getArtistId());		
        
		if (yahooArtist == null) {
			 throw new NotFoundException("Artist " + album.getArtist() + " artistId " 
					                     + album.getArtistId() + " was not found.");
		}
		
		if (albumsByArtist.getStart() == 0) {
            Future<AmazonAlbumData> amazonAlbum;
            if (album.getAlbum() != null && album.getArtist() != null)
            	amazonAlbum = amazonAlbumCache.getAsync(new AlbumAndArtist(album.getAlbum(), album.getArtist()));
            else
            	amazonAlbum = null;
            Future<List<? extends YahooSongData>> albumTracks = yahooAlbumSongsCache.getAsync(album.getAlbumId());

		    AlbumView albumView = getAlbumView(viewpoint, song, album, amazonAlbum, albumTracks);
	        view.addAlbum(albumView);
		}
		
        fillAlbumsByArtist(viewpoint, yahooArtist, albumsByArtist, album, view);
	    
		return view;
	}
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		TrackHistory current = getCurrentTrack(viewpoint, user);
		return getTrackView(current.getTrack(), current);
	}

	public Future<TrackView> getTrackViewAsync(long trackId) {
		// called for side effect to kick off querying the results.
		Track track = em.find(Track.class, trackId);
		if (track == null)
			throw new RuntimeException("database isolation bug (?): can't reattach trackid in hintNeedsRefresh");
		return getTrackViewAsync(track);
	}
	
	public Future<TrackView> getTrackViewAsync(Track track) {
		FutureTask<TrackView> futureView =
			new FutureTask<TrackView>(new GetTrackViewTask(track, null));
		getThreadPool().execute(futureView);
		return futureView;
	}
	
	public Future<TrackView> getTrackViewAsync(TrackHistory trackHistory) {
		FutureTask<TrackView> futureView =
			new FutureTask<TrackView>(new GetTrackViewTask(trackHistory.getTrack(), trackHistory));
		getThreadPool().execute(futureView);
		return futureView;
	}
	
	private List<TrackView> getViewsFromTrackHistories(Viewpoint viewpoint, List<TrackHistory> tracks, boolean includePersonMusicPlay) {
		
		logger.debug("Getting TrackViews from tracks list with {} items", tracks.size());
		
		// spawn a bunch of yahoo updater threads in parallel
		Map<TrackHistory, Future<TrackView>> futureViews = new HashMap<TrackHistory, Future<TrackView>>(tracks.size());
		for (TrackHistory t : tracks) {
			futureViews.put(t, getTrackViewAsync(t));
		}
	
        // now harvest all the results
		List<TrackView> views = new ArrayList<TrackView>(futureViews.size());
		for (TrackHistory t : tracks) {
			TrackView v;
			v = ThreadUtils.getFutureResult(futureViews.get(t)); // throws on exception in the thread
			if (includePersonMusicPlay) {
			    // add the person who made this "track history" 
		        List<PersonMusicPlayView> personMusicPlayViews = new ArrayList<PersonMusicPlayView>();
			    personMusicPlayViews.add(new PersonMusicPlayView(personViewer.getPersonView(viewpoint, t.getUser()), t.getLastUpdated()));
			    v.setPersonMusicPlayViews(personMusicPlayViews);
			}
			
			assert v != null;
			
			views.add(v);
		}
		
		return views;
	}

	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) {
		//logger.debug("getLatestTrackViews() for user {}", user);
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, 0, maxResults);
		return getViewsFromTrackHistories(viewpoint, history, false);
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
	
	private Track getMatchingTrack(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException {
		// max results of 1 picks one of the matching Track arbitrarily
		Query q = buildSongQuery(viewpoint, artist, album, name, 1);
		
		try {
			Track t = (Track) q.getSingleResult();
			return t;
		} catch (NoResultException e) {
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
		logger.debug("artist page search for '{}'", artist);
		// FIXME exception catch is a hack that should be removed once we're pretty confident 
	    // the music system is robust		
        try {
			// albumsByArtist is a pageable object that contains information on what albums the expanded
	        // artist view should be loaded with, it also needs to have these albums set in its results field
			ExpandedArtistView artistView = getExpandedArtistView(viewpoint, artist, albumsByArtist);
			artistView.pageAlbums(albumsByArtist);
			return artistView;
        } catch (RuntimeException e) {
        	logger.error("artist-based expandedArtistSearch hit an exception", e);
        	throw new NotFoundException("Not found due to an exception in the music system.");
        }
	}

	private ExpandedArtistView expandedArtistSearchInTrackHistory(Viewpoint viewpoint, String artist, String album, String name, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		Track track = getMatchingTrack(viewpoint, artist, album, name);

		List<? extends YahooSongData> songs = yahooSongCache.getSync(track);
		if (songs.isEmpty())
			throw new NotFoundException("No Yahoo! songs found for track " + track);
		
		// get the primary artist result for this artist, than make sure we use the song result
		// associated with the primary artist result, if possible.
		YahooArtistData yahooArtist = yahooArtistCache.getSyncByName(track.getArtist());
		
		YahooSongData yahooSong = null;
		
		if (yahooArtist != null) {
		    for (YahooSongData songForTrack : songs) {
		    	if (songForTrack.getArtistId().equals(yahooArtist.getArtistId())) {
		    		yahooSong = songForTrack;
		    		break;
		    	}
		    }
		}

		// but we'd rather use any song result than none
		if (yahooSong == null) {
			yahooSong = songs.get(0);
		}
		
	   // information about the album the song is on
       YahooAlbumData yahooAlbum = yahooAlbumCache.getSync(yahooSong.getAlbumId());
       if (yahooAlbum == null)
    	   throw new NotFoundException("Yahoo! returned no album for song " + yahooSong + " with album id " + yahooSong.getAlbumId());
       
        // albumsByArtist is a pageable object that contains information on what albums the expanded
        // artist view should be loaded with, it also needs to have these albums set in its results field
        YahooSongData songToExpand = null;
        if (name != null) {
    	    //this is a search based on a song name, not just an album search
    	    songToExpand = yahooSong;
        }
        
	    ExpandedArtistView artistView = getExpandedArtistView(viewpoint, songToExpand, yahooAlbum, albumsByArtist);
 	    artistView.pageAlbums(albumsByArtist);
        return artistView;		
	}
	
	private ExpandedArtistView expandedArtistSearchInCachedAlbumListing(Viewpoint viewpoint, String artist, String album, String name, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		YahooAlbumData yahooAlbum = yahooArtistAlbumsCache.findAlreadyCachedAlbum(artist, album, name);
		ExpandedArtistView artistView = getExpandedArtistView(viewpoint, null, yahooAlbum, albumsByArtist);
		artistView.pageAlbums(albumsByArtist);
	    return artistView;		
	}
	
	public ExpandedArtistView expandedArtistSearch(Viewpoint viewpoint, String artist, String album, String name, Pageable<AlbumView> albumsByArtist) throws NotFoundException {
		logger.debug("artist page search for '{}' '{}' '" + name + "'", artist, album);
		if (artist == null && album == null && name == null) {
			throw new NotFoundException("Search has no parameters");
		}
		
		if (album == null && name == null) {
			// just return the artist information (starting with the first album or whatever) 
			return expandedArtistSearch(viewpoint, artist, albumsByArtist);
		}
	
		try {
			return expandedArtistSearchInTrackHistory(viewpoint, artist, album, name, albumsByArtist);
		} catch (NotFoundException e) { 
			logger.debug("expandedArtistSearch in track history failed: {}  (will now fall back to search in cached album listings)", e.getMessage());
			return expandedArtistSearchInCachedAlbumListing(viewpoint, artist, album, name, albumsByArtist);
		}
	}

	private static final int MAX_RELATED_PEOPLE_RESULTS = 3;
	
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
            	    selfMusicPlayView = new PersonMusicPlayView(personViewer.getPersonView(viewpoint, user), h.getLastUpdated());
                }
            	continue;
            }

            if (!contacts.isEmpty()) {
                // we are filling out the views with person's contacts
            	if (contacts.contains(user)) {          	
            		// it is correct to use the first track history that related to a given contact because we are interested
            		// in the most recent play and track histories were sorted by the time of the last update
            		if (views.get(user) == null) {
            			views.put(user, new PersonMusicPlayView(personViewer.getPersonView(viewpoint, user), h.getLastUpdated()));
            		}
            	} else {
            		if (extraViews.get(user) == null) {    			
            			extraViews.put(user, new PersonMusicPlayView(personViewer.getPersonView(viewpoint, user), h.getLastUpdated()));
              		}            		
            	}
            } else {
        		if (views.get(user) == null) {
        			views.put(user, new PersonMusicPlayView(personViewer.getPersonView(viewpoint, user), h.getLastUpdated()));            			
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
	
	public TrackSearchResult searchTracks(Viewpoint viewpoint, String queryString) {
		final String[] fields = { "artist", "album", "name" };
		QueryParser queryParser = new MultiFieldQueryParser(fields, TrackIndexer.getInstance().createAnalyzer());
		queryParser.setDefaultOperator(Operator.AND);
		org.apache.lucene.search.Query query;
		try {
			query = queryParser.parse(queryString);
			
			Hits hits = TrackIndexer.getInstance().getSearcher().search(query);
			
			return new TrackSearchResult(hits);
			
		} catch (org.apache.lucene.queryParser.ParseException e) {
			return new TrackSearchResult("Don't understand the search '" + queryString + "'");
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

	// FIXMEFIXMEFIXME - this needs to be annotated to mandate no transaction,
    // since it can deadlock if called for the same track twice in the same transaction
	//
	// Alternatively, and maybe better, unify the last segment of code with 
	// setCurrentTrack and make adding tracks to the track database always async.
	//
	public void onExternalAccountFeedEntry(final User user, ExternalAccount external, FeedEntry feedEntry, int entryPosition) {
		if (!external.hasLovedAndEnabledType(ExternalAccountType.RHAPSODY))
			return;
		
		if (!(feedEntry instanceof TrackFeedEntry)) {
			logger.warn("Rhapsody account {} has non-TrackFeedEntry {}", external, feedEntry);
			return;
		}

		TrackFeedEntry entry = (TrackFeedEntry) feedEntry;
		
		final Map<String,String> properties = new HashMap<String,String>();
		properties.put("type", ""+TrackType.NETWORK_STREAM);
		
		// Rhapsody has a tendency to leave stuff blank, e.g. artist and duration are known to be sometimes blank
		String name = entry.getTrack();
		String album = entry.getAlbum();
		String url = entry.getPlayHref();
		String duration = entry.getDuration();
		String artist = entry.getArtist();
		if (artist == null || artist.trim().length() == 0) {
			logger.debug("Rhapsody feed has blank artist field");
			artist = "Unknown";
		}
		
		properties.put("name", name);
		properties.put("artist", artist);
		properties.put("album", album);
		properties.put("url", url);
		
		if (duration != null && duration.trim().length() > 0) {
			try {
				Integer durationInt = Integer.parseInt(duration);
				durationInt = durationInt/1000;  // Rhapsody duration info is in milliseconds, elswhere it's seconds
			
				properties.put("duration", ""+duration);
			} catch (NumberFormatException e) {
				logger.debug("bad duration in rhapsody feed '{}'", duration);
				// just don't store the duration
			}
		} else {
			logger.debug("Rhapsody feed has empty duration field");
			// just don't store the duration
		}
		
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
		final long virtualPlayTime = now - (1000 * 60 * entryPosition);
	
		// We need this track to be committed before the end of our current 
		// transaction - conceptually, right now we should be in POJO code, 
		// not inside any transaction most likely, FIXME
		// FIXME retry on db errors
		final long trackId = getTrackIdIsolated(properties);
		
		// Then we need another transaction because this outer transaction
		// can't see the new track object due to isolation
		// FIXME retry on db errors ?
		runner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				Track t = em.find(Track.class, trackId);
				if (t == null)
					throw new RuntimeException("database isolation mistake (?): null track " + trackId);
				User u = em.find(User.class, user.getId());
				if (u == null)
					throw new RuntimeException("failed to reattach user");
				addTrackHistory(u, t, new Date(virtualPlayTime), false);
			}
		});
	}
	
	public long getLatestPlayTime(Viewpoint viewpoint, User user) {
		List<TrackHistory> history = getTrackHistory(viewpoint, user, History.LATEST, 0, 1);
		if (history.isEmpty())
			return 0;
		else
			return history.get(0).getLastUpdated().getTime();
	}
}
