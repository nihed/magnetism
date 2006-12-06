package com.dumbhippo.mbean;

import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.LastFmUpdater;
import com.dumbhippo.server.util.EJBUtil;

public class LastFmUpdaterPeriodicJob extends ExternalAccountUpdaterPeriodicJob {

	public static final long POLL_FREQUENCY = 1000 * 60 * 23; // 23 minutes, prime number not used elsewhere
	
	public long getFrequencyInMilliseconds() {
		return POLL_FREQUENCY;
	}
	
	@Override
	protected boolean enabled() {
		Configuration config = EJBUtil.defaultLookup(Configuration.class);		
		return !config.isFeatureEnabled("pollingTask");
	}

	@Override
	public String getName() {
		return "Last.fm";
	}

	@Override
	protected Class<? extends CachedExternalUpdater<?>> getUpdater() {
		return LastFmUpdater.class;
	}
}
