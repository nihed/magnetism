package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;

@Local
public interface MusicSystem {
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;
		
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
}
