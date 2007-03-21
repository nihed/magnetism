package com.dumbhippo.persistence;

import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.FlickrUpdater;
import com.dumbhippo.server.LastFmUpdater;
import com.dumbhippo.server.MySpaceUpdater;
import com.dumbhippo.server.PicasaUpdater;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;

/**
 * Represents the "family" of a polling task.  Persisted in the database;
 * do not reorder.
 *
 * @author walters
 */
public enum PollingTaskFamilyType {
	FEED() { // 0
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return FeedSystem.class;
		}
	}, 
	LASTFM() { // 1
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return LastFmUpdater.class;
		}
	}, 
	FLICKR() { // 2
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return FlickrUpdater.class;
		}
	},
	YOUTUBE() { // 3
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return YouTubeUpdater.class;
		}
	},
	MYSPACE() { // 4
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return MySpaceUpdater.class;
		}		
	},
	PICASA() { // 5
		@Override
		public Class<? extends PollingTaskLoader> getLoader() {
			return PicasaUpdater.class;
		}
	};
	
	public abstract Class<? extends PollingTaskLoader> getLoader();
}
