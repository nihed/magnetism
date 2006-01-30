package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;

@Local
public interface MusicSystem {
	
	public TrackView getCurrentTrackView(Viewpoint viewpoint, User user) throws NotFoundException;
		
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, User user, int maxResults) throws NotFoundException;
	
	public List<TrackView> getLatestTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException;
	
	public List<TrackView> getFrequentTrackViews(Viewpoint viewpoint, Group group, int maxResults) throws NotFoundException;
	
	public TrackView songSearch(Viewpoint viewpoint, String artist, String album, String name) throws NotFoundException;
	
	public List<PersonMusicView> getRelatedPeople(Viewpoint viewpoint, String artist, String album, String name);
	
	public List<TrackView> getRecommendations(Viewpoint viewpoint, String artist, String album, String name);
}
