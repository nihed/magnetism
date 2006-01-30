package com.dumbhippo.server.impl;

import java.util.List;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonMusicView;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.Viewpoint;

/**
 * Originally there was one stateless bean implementing MusicSystem and MusicSystemInternal;
 * that seemed to not work, so I created this delegating bean.
 * 
 * @author Havoc Pennington
 *
 */
@Stateless
public class MusicSystemBean implements MusicSystem {

	@EJB
	private MusicSystemInternal internal;
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException {
		return internal.getCurrentTrackView(viewpoint, user);
	}

	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException {
		return internal.getLatestTrackViews(viewpoint, user, maxResults);
	}

	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException {
		return internal.getFrequentTrackViews(viewpoint, user, maxResults);
	}

	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException {
		return internal.getLatestTrackViews(viewpoint, group, maxResults);
	}

	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException {
		return internal.getFrequentTrackViews(viewpoint, group, maxResults);
	}

	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException {
		return internal.songSearch(viewpoint, artist, album, name);
	}

	public List<PersonMusicView> getRelatedPeople(Viewpoint viewpoint, String artist, String album, String name) {
		return internal.getRelatedPeople(viewpoint, artist, album, name);
	}

	public List<TrackView> getRecommendations(Viewpoint viewpoint, String artist, String album, String name) {
		return internal.getRecommendations(viewpoint, artist, album, name);
	}
}
