package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.services.YouTubeVideo;

/** 
 * This bean caches YouTube video feeds.
 * 
 * @author Colin Walters
 */
@Local
public interface YouTubeUpdater extends CachedExternalUpdater<YouTubeUpdateStatus> {	
	public void saveUpdatedStatus(String username, List<? extends YouTubeVideo> videos);
}
