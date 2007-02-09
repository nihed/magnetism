package com.dumbhippo.server.blocks;

import java.util.List;

import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.TrackView;

public interface MusicBlockView {
	TrackView getTrack();
	List<TrackView> getOldTracks();
	ChatMessageView getLastMessage();
	List<ChatMessageView> getRecentMessages();
	boolean isQuip();
}
