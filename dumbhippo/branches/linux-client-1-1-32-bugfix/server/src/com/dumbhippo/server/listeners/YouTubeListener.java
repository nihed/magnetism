package com.dumbhippo.server.listeners;

import java.util.List;

import com.dumbhippo.services.YouTubeVideo;

public interface YouTubeListener {
	public void onYouTubeRecentVideosChanged(String flickrId, List<? extends YouTubeVideo> videos);
}
