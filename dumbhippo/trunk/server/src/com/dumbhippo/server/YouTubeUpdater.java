package com.dumbhippo.server;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.services.YouTubeVideo;

/** 
 * This bean caches YouTube video feeds.
 * 
 * @author Colin Walters
 */
@Local
public interface YouTubeUpdater extends CachedExternalUpdater<YouTubeUpdateStatus> {
	public Set<String> getActiveYouTubeUsers();
	public Collection<User> getUsersWhoLoveYouTubeAccount(String username);
	
	public void saveUpdatedStatus(String username, List<YouTubeVideo> videos);
}
