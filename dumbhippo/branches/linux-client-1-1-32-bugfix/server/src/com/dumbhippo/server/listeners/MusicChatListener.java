package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.TrackMessage;

public interface MusicChatListener {
	public void onTrackMessageCreated(TrackMessage message);
}
