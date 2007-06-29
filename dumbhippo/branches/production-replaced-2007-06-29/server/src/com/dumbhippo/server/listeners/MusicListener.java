package com.dumbhippo.server.listeners;

import java.util.Date;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.User;

public interface MusicListener {
	public void onTrackPlayed(User user, Track track, Date when);
}
