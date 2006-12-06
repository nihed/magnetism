package com.dumbhippo.persistence;

import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.LastFmUpdater;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;

/**
 * Represents the "family" of a polling task.  Persisted in the database;
 * do not reorder.
 *
 * @author walters
 */
public enum PollingTaskFamilyType {
	FEED() {
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return FeedSystem.class;
		}
	}, 
	LASTFM() {
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return LastFmUpdater.class;
		}
	};
	
	public abstract Class<? extends PollingTaskLoader> getLoader();
}
