package com.dumbhippo.server.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.LastFmUpdateStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.LastFmUpdater;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
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

	public void saveUpdatedStatus(final String username, final List<LastFmTrack> tracks) {
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
		}
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
}
