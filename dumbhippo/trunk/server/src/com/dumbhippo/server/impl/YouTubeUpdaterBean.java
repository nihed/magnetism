package com.dumbhippo.server.impl;

import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.YouTubeUpdateStatus;
import com.dumbhippo.polling.PollResult;
import com.dumbhippo.polling.PollingTask;
import com.dumbhippo.polling.PollingTaskFamily;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.YouTubeUpdater;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.YouTubeVideo;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.CacheFactoryBean;
import com.dumbhippo.services.caches.YouTubeVideosCache;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class YouTubeUpdaterBean extends CachedExternalUpdaterBean<YouTubeUpdateStatus> implements CachedExternalUpdater<YouTubeUpdateStatus>, YouTubeUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(YouTubeUpdaterBean.class);
	
	@EJB
	private Notifier notifier;

	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
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
		
		TxUtils.assertHaveTransaction();
		
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

		public long getDefaultPeriodicitySeconds() {
			return 20 * 60; // 20 minutes
		}
		
		public String getName() {
			return PollingTaskFamilyType.YOUTUBE.name();
		}
		
		public long rescheduleSeconds(long suggestedSeconds) {
			return suggestedSeconds;
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
			YouTubeVideosCache cache = CacheFactoryBean.defaultLookup(YouTubeVideosCache.class);
			
			final List<? extends YouTubeVideo> videos = cache.getSync(username, true);
			
			return TxUtils.runInTransaction(new Callable<PollResult>() {
				public PollResult call() {
					DataService.getModel().initializeReadWriteSession(SystemViewpoint.getInstance());
					
					YouTubeUpdater proxy = EJBUtil.defaultLookup(YouTubeUpdater.class);
					boolean changed = proxy.saveUpdatedStatus(username, videos);
					
					return new PollResult(changed, false);
				}
			});
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
