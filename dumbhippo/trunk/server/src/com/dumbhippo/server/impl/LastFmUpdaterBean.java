package com.dumbhippo.server.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.mbean.DynamicPollingSystem;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTaskFamily;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.LastFmUpdateStatus;
import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.LastFmUpdater;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.LastFmTrack;
import com.dumbhippo.services.LastFmWebServices;
import com.dumbhippo.services.TransientServiceException;

@Stateless
public class LastFmUpdaterBean extends CachedExternalUpdaterBean<LastFmUpdateStatus> implements LastFmUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(LastFmUpdaterBean.class);
	
	@EJB
	private MusicSystemInternal musicSystem;
	
	@EJB
	private PollingTaskPersistence pollingPersistence;	

	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)	
	public void doPeriodicUpdate(String username) {
		LastFmUpdater proxy = EJBUtil.defaultLookup(LastFmUpdater.class);
		
		List<LastFmTrack> tracks;
		try {
			tracks = LastFmWebServices.getTracksForUser(username);
		} catch (TransientServiceException e) {
			logger.warn("Exception from LastFmWebServices", e);
			return;
		}
		proxy.saveUpdatedStatus(username, tracks);
	}
	
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
		
		if (!updateStatus.getSongHash().equals(hash)) {
			final String previousHash = updateStatus.getSongHash();
			logger.debug("Most recent tracks changed '{}' -> '{}'",
					updateStatus.getSongHash(), hash);
			updateStatus.setSongHash(hash);
			for (User user : getAccountLovers(username)) {
				// Tracks are in most to least recent order, walk in reverse
				for (int i = tracks.size() - 1; i >= 0; i--) {
					LastFmTrack lastFmTrack = tracks.get(i);
					if (computeTrackHash(lastFmTrack).equals(previousHash))
						break; // Stop when we get to the last track we saw
					Map<String, String> props = lastFmTrackToProps(lastFmTrack);
					if (i == 0) {
						musicSystem.setCurrentTrack(user, props);
					} else {
						musicSystem.addHistoricalTrack(user, props);
					}
				}
				musicSystem.queueMusicChange(user.getGuid());
			}
			return true;
		}
		return false;
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

		public long getDefaultPeriodicity() {
			return 5 * 60 * 1000; // 5 minutes
		}

		public long getMaxOutstanding() {
			return 2;
		}

		public long getMaxPerSecond() {
			return 1;
		}

		public String getName() {
			return PollingTaskFamilyType.LASTFM.name();
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
				throw new DynamicPollingSystem.PollingTaskNormalExecutionException(e);
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
	
	public Set<PollingTask> loadTasks(Set<PollingTaskEntry> entries) {
		Set<PollingTask> tasks = new HashSet<PollingTask>();
		for (PollingTaskEntry entry : entries) {
			String username = entry.getTaskId();
			tasks.add(new LastFmTask(username));
		}
		return tasks;		
	}

	public void migrateTasks() {
		for (String username : getActiveUsers()) {
			pollingPersistence.createTask(PollingTaskFamilyType.LASTFM, username);
		}		
	}
}
