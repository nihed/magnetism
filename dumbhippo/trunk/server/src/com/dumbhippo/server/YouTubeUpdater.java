package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.server.listeners.ExternalAccountsListener;
import com.dumbhippo.services.YouTubeVideo;

/** 
 * This bean caches YouTube video feeds.
 * 
 * @author Colin Walters
 */
@Local
public interface YouTubeUpdater extends ExternalAccountsListener {

	public YouTubeUpdateStatus getCachedStatus(User user) throws NotFoundException;
	public YouTubeUpdateStatus getCachedStatus(ExternalAccount external) throws NotFoundException;
	public YouTubeUpdateStatus getCachedStatus(String username) throws NotFoundException;
	public Set<String> getActiveYouTubeUsers();

	public void periodicUpdate(String username);

	public void saveUpdatedStatus(String username, List<YouTubeVideo> videos);
}
