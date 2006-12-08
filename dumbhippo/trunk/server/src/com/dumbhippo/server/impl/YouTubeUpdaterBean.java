package com.dumbhippo.server.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTaskFamily;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.services.caches.YouTubeVideosCache;

@Stateless
public class YouTubeUpdaterBean extends CachedExternalUpdaterBean<YouTubeUpdateStatus> implements CachedExternalUpdater<YouTubeUpdateStatus>, YouTubeUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(YouTubeUpdaterBean.class);
	
	@EJB
	private Notifier notifier;

	@WebServiceCache
	private YouTubeVideosCache videosCache;	
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)	
	public void doPeriodicUpdate(String username) {
		EJBUtil.assertNoTransaction();
		
		YouTubeUpdater proxy = EJBUtil.defaultLookup(YouTubeUpdater.class);
		
		List<? extends YouTubeVideo> videos = videosCache.getSync(username, true);
		proxy.saveUpdatedStatus(username, videos);
	}
	
	//TODO: Identify the unique part of youtube urls instead of using the whole thing
	private String computeHash(List<? extends YouTubeVideo> videos) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= videos.size())
				break;
			sb.append(videos.get(i).getThumbnailHref());
		}
		return sb.toString();
	}	

	public boolean saveUpdatedStatus(String username, List<? extends YouTubeVideo> videos) {
		logger.debug("Saving new YouTube status for " + username + ": videos {}",
				videos);
		
		EJBUtil.assertHaveTransaction();
		
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
			return true;
		}
		return false;
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

	@Override
	protected PollingTaskFamilyType getTaskFamily() {
		return PollingTaskFamilyType.YOUTUBE;
	}

	private static class YouTubeTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicity() {
			return 20 * 60 * 1000; // 20 minutes
		}

		// Rough numbers, hopefully they're reasonable
		public long getMaxOutstanding() {
			return 10;
		}

		public long getMaxPerSecond() {
			return 5;
		}

		public String getName() {
			return PollingTaskFamilyType.YOUTUBE.name();
		}
	}
	
	private static PollingTaskFamily family = new YouTubeTaskFamily();
	
	private static class YouTubeTask extends PollingTask {
		private String username;
		
		public YouTubeTask(String username) {
			this.username = username;
		}

		@Override
		protected PollResult execute() throws Exception {
			boolean changed = false;
			YouTubeUpdater proxy = EJBUtil.defaultLookup(YouTubeUpdater.class);
			YouTubeVideosCache cache = EJBUtil.defaultLookup(YouTubeVideosCache.class);
			
			List<? extends YouTubeVideo> videos = cache.getSync(username, true);
			changed = proxy.saveUpdatedStatus(username, videos);			
			return new PollResult(changed, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return username;
		}
	}	
	
	@Override
	protected PollingTask createPollingTask(String handle) {
		return new YouTubeTask(handle);
	}
}
