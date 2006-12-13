package com.dumbhippo.mbean;

import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.YouTubeUpdater;

public class YouTubeUpdaterPeriodicJob extends ExternalAccountUpdaterPeriodicJob {
	public static final long YOUTUBE_POLL_FREQUENCY = 1000 * 60 * 17; // 17 minutes, prime number not used elsewhere

	@Override
	protected Class<? extends CachedExternalUpdater<?>> getUpdater() {
		return YouTubeUpdater.class;
	}

	// keep this short/one-wordish since it's in all the log messages
	@Override
	public String getName() {
		return "YouTube";
	}

	public long getFrequencyInMilliseconds() {
		return YOUTUBE_POLL_FREQUENCY;		
	}
}
