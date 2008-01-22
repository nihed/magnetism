package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.LastFmUpdateStatus;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.polling.PollResult;
import com.dumbhippo.polling.PollingTask;
import com.dumbhippo.polling.PollingTaskFamily;
import com.dumbhippo.polling.PollingTaskNormalExecutionException;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.LastFmUpdater;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.LastFmTrack;
import com.dumbhippo.services.LastFmWebServices;
import com.dumbhippo.services.TransientServiceException;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class LastFmUpdaterBean extends CachedExternalUpdaterBean<LastFmUpdateStatus> implements LastFmUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(LastFmUpdaterBean.class);
	
	@EJB
	private MusicSystem musicSystem;

	private String computeTrackHash(LastFmTrack track) {
		return track.getUrl() + " " + track.getListenTime();		
	}
	
	private String computeHash(List<LastFmTrack> tracks) {
		StringBuilder sb = new StringBuilder();
		// We're only interested if the most recent song changed
		if (tracks.size() > 0) {
			sb.append(computeTrackHash(tracks.get(0)));
		}
		return sb.toString();
	}	
	
	private Map<String,String> lastFmTrackToProps(LastFmTrack lastFmTrack) {
		Map<String,String> props = new HashMap<String, String>();
		props.put("artist", lastFmTrack.getArtist());
		props.put("name", lastFmTrack.getTitle());
		props.put("url", lastFmTrack.getUrl());
		return props;
	}

	private void addNewTracks(String username, List<LastFmTrack> tracks, String previousHash) throws RetryException {
		long mostRecentListenTimeSeconds = -1;
		for (LastFmTrack track : tracks) {
			if (mostRecentListenTimeSeconds == -1 || track.getListenTime() > mostRecentListenTimeSeconds)
				mostRecentListenTimeSeconds = track.getListenTime();
		}
		
		Date lastLastFmUpdate = new Date(mostRecentListenTimeSeconds * 1000);			
		for (User user : getAccountLovers(username)) {
			// Tracks are in most to least recent order, walk in reverse
			Date lastNativeUpdate = user.getAccount().getNativeMusicSharingTimestamp();
			// Don't add these tracks if we've gotten "recent" updates natively
			if (lastNativeUpdate != null
					&& (lastLastFmUpdate.before(lastNativeUpdate) || 
					  (lastLastFmUpdate.getTime() - lastNativeUpdate.getTime()) < MusicSystem.NATIVE_MUSIC_OVERRIDE_TIME_MS)) {
				logger.debug("Ignoring music update due to recent native update");
				continue;
			}
			
			long now = System.currentTimeMillis();
			for (int i = tracks.size() - 1; i >= 0; i--) {
				LastFmTrack lastFmTrack = tracks.get(i);
				if (computeTrackHash(lastFmTrack).equals(previousHash))
					break; // Stop when we get to the last track we saw
				Map<String, String> props = lastFmTrackToProps(lastFmTrack);
				
				// The data we get from last.fm/audioscrobbler simply includes the 
				// literal date provided by the audioscrobbler client. If the audioscrobbler
				// client is broken or the user's clock is off, this might be in the
				// future. Stacking blocks in the future is bad, since (among other things)
				// it will make the user *always* the most recently active user on the system.
				// So clamp play times to the current time.
				long trackListenTime = lastFmTrack.getListenTime() * 1000;
				long listenTime = trackListenTime <= now ? trackListenTime : now;
				
				musicSystem.addHistoricalTrack(user, props, listenTime, false);
			}
		}
	}

	public boolean saveUpdatedStatus(final String username, final List<LastFmTrack> tracks) {
		logger.debug("Saving new Last.fm status for " + username + ": tracks {}", tracks);
		
		LastFmUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(username);
		} catch (NotFoundException e) {
			updateStatus = new LastFmUpdateStatus(username);
			em.persist(updateStatus);
		}

		String hash = computeHash(tracks);
		
		if (updateStatus.getSongHash().equals(hash))
			return false;
		
		final String previousHash = updateStatus.getSongHash();
		logger.debug("Most recent tracks changed '{}' -> '{}'",
				updateStatus.getSongHash(), hash);
		updateStatus.setSongHash(hash);

		TxUtils.runInTransactionOnCommit(new TxRunnable() {
			public void run() throws RetryException {
				/* Using SystemViewpoint here is a little dubious, but there is no actual access
				 * control involved and it saves us having to do deal with the fact that we are 
				 * adding tracks for multiple users if multiple users have the same last.fm
				 * username in their account.  
				 */
				DataService.getModel().initializeReadWriteSession(SystemViewpoint.getInstance());
				addNewTracks(username, tracks, previousHash);
			}
		});
		
		return true;
	}
	
	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.LASTFM;
	}

	@Override
	protected Query getCachedStatusQuery(String handle) {
		Query q = em.createQuery("SELECT updateStatus FROM LastFmUpdateStatus updateStatus " +
		"WHERE updateStatus.username = :username");
		q.setParameter("username", handle);
		return q;
	}

	@Override
	protected Class<? extends CachedExternalUpdater<LastFmUpdateStatus>> getUpdater() {
		return LastFmUpdater.class;
	}

	@Override
	protected PollingTaskFamilyType getTaskFamily() {
		return PollingTaskFamilyType.LASTFM;
	}

	private static class LastFmTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicitySeconds() {
			return 15 * 60; // 15 minutes
		}

		public String getName() {
			return PollingTaskFamilyType.LASTFM.name();
		}

		public long rescheduleSeconds(long suggestedSeconds) {
			return suggestedSeconds;
		}
	}
	
	private static PollingTaskFamily family = new LastFmTaskFamily();
	
	private static class LastFmTask extends PollingTask {
		private String username;
		
		public LastFmTask(String username) {
			this.username = username;
		}

		@Override
		protected PollResult execute() throws Exception {
			boolean changed = false;
			List<LastFmTrack> tracks;
			try {
				tracks = LastFmWebServices.getTracksForUser(username);
			} catch (TransientServiceException e) {
				throw new PollingTaskNormalExecutionException(e);
			}
			
			LastFmUpdater updater = EJBUtil.defaultLookup(LastFmUpdater.class);
			changed = updater.saveUpdatedStatus(username, tracks); 
			return new PollResult(changed, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return username;
		}
	}	
	
	@Override
	protected PollingTask createPollingTask(String handle) {
		return new LastFmTask(handle);
	}
}
