package com.dumbhippo.server;

import java.util.Map;

import javax.ejb.Local;

import com.dumbhippo.persistence.CurrentTrack;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;

@Local
public interface MusicSystem {

	public Track getTrack(Map<String,String> properties);
	
	public void setCurrentTrack(User user, Track track);

	public void setCurrentTrack(User user, Map<String,String> properties);
	
	public CurrentTrack getCurrentTrack(Viewpoint viewpoint, User user) throws NotFoundException;
}
