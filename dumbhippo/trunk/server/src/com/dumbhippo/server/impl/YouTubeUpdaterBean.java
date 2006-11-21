package com.dumbhippo.server.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.caches.YouTubeVideosCache;

@Stateless
public class YouTubeUpdaterBean extends CachedExternalUpdaterBean<YouTubeUpdateStatus> implements YouTubeUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(YouTubeUpdaterBean.class);

	@EJB
	private YouTubeVideosCache videosCache;
	
	@EJB
	private Notifier notifier;
	
	public Set<String> getActiveYouTubeUsers() {
		return getActiveUsers();
	}
	
	public Collection<User> getUsersWhoLoveYouTubeAccount(String username) {
		return getAccountLovers(username);
	}	

	@Override
	public void doPeriodicUpdate(String username) {
		EJBUtil.assertNoTransaction();
		YouTubeUpdater proxy = EJBUtil.defaultLookup(YouTubeUpdater.class);
		
		videosCache.expireCache(username);
		List<YouTubeVideo> videos = videosCache.getSync(username);
		proxy.saveUpdatedStatus(username, videos);
	}
	
	//TODO: Identify the unique part of youtube urls instead of using the whole thing
	private String computeHash(List<YouTubeVideo> videos) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= videos.size())
				break;
			sb.append(videos.get(i).getThumbnailHref());
		}
		return sb.toString();
	}	

	public void saveUpdatedStatus(String username, List<YouTubeVideo> videos) {
		logger.debug("Saving new YouTube status for " + username + ": videos {}",
				videos);
		
		YouTubeUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(username);
		} catch (NotFoundException e) {
			updateStatus = new YouTubeUpdateStatus(username);
			em.persist(updateStatus);
		}

		String hash = computeHash(videos);
		
		if (!updateStatus.getVideoHash().equals(hash)) {
			logger.debug("Most recent videos changed '{}' -> '{}'",
					updateStatus.getVideoHash(), hash);
			updateStatus.setVideoHash(hash);
			notifier.onYouTubeRecentVideosChanged(username, videos);
		}
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.YOUTUBE;
	}

	@Override
	protected Query getCachedStatusQuery(String handle) {
		Query q = em.createQuery("SELECT updateStatus FROM YouTubeUpdateStatus updateStatus " +
		"WHERE updateStatus.username = :username");
		q.setParameter("username", handle);
		return q;
	}

	@Override
	protected Class<? extends CachedExternalUpdater<YouTubeUpdateStatus>> getUpdater() {
		return YouTubeUpdater.class;
	}
}
