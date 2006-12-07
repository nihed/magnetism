package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.services.YouTubeVideo;

/** 
 * This bean caches YouTube video feeds.
 * 
 * @author Colin Walters
 */
@Local
public interface YouTubeUpdater extends CachedExternalUpdater<YouTubeUpdateStatus>, PollingTaskLoader {	
	public boolean saveUpdatedStatus(String username, List<? extends YouTubeVideo> videos);
}
