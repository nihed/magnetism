package com.dumbhippo.mbean;

import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.FlickrUpdater;

public class FlickrUpdaterPeriodicJob extends ExternalAccountUpdaterPeriodicJob {

	public static final long FLICKR_POLL_FREQUENCY = 1000 * 60 * 13; // 13 minutes, prime number not used elsewhere
	
	public long getFrequencyInMilliseconds() {
		return FLICKR_POLL_FREQUENCY;
	}

	@Override
	public String getName() {
		return "flickr";
	}

	@Override
	protected Class<? extends CachedExternalUpdater<?>> getUpdater() {
		return FlickrUpdater.class;
	}
}
