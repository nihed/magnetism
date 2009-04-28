package com.dumbhippo.server.impl;

import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.SmugmugUpdateStatus;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.polling.PollResult;
import com.dumbhippo.polling.PollingTask;
import com.dumbhippo.polling.PollingTaskFamily;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.SmugmugUpdater;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.smugmug.rest.bind.Image;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.CacheFactoryBean;
import com.dumbhippo.services.caches.SmugmugAlbumsCache;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class SmugmugUpdaterBean extends CachedExternalUpdaterBean<SmugmugUpdateStatus> 
  implements CachedExternalUpdater<SmugmugUpdateStatus>, SmugmugUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SmugmugUpdaterBean.class);
	
	@EJB
	private Notifier notifier;

	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	private String computeHash(List<? extends Image> albums) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= albums.size())
				break;
			sb.append(albums.get(i).getThumbURL());
		}
		return Digest.computeDigestMD5(sb.toString());
	}	

	public boolean saveUpdatedStatus(String username, List<? extends Image> albums) {
		logger.debug("Saving new Smugmug status for " + username + ": albums {}",
				albums);
		
		TxUtils.assertHaveTransaction();
		
		SmugmugUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(username);
		} catch (NotFoundException e) {
			updateStatus = new SmugmugUpdateStatus(username);
			em.persist(updateStatus);
		}

		String hash = computeHash(albums);
		
		if (!updateStatus.getAlbumHash().equals(hash)) {
			logger.debug("Most recent albums changed '{}' -> '{}'",
					updateStatus.getAlbumHash(), hash);
			updateStatus.setAlbumHash(hash);
			notifier.onSmugmugRecentAlbumsChanged(username, albums);
			return true;
		}
		return false;
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.SMUGMUG;
	}

	@Override
	protected Query getCachedStatusQuery(String handle) {
		Query q = em.createQuery("SELECT updateStatus FROM SmugmugUpdateStatus updateStatus " +
		"WHERE updateStatus.username = :username");
		q.setParameter("username", handle);
		return q;
	}
	
	@Override
	public SmugmugUpdateStatus getCachedStatus(String handle) throws NotFoundException 
	{
		Query q = getCachedStatusQuery(handle);
		SmugmugUpdateStatus st = null;
		try {
			st =  (SmugmugUpdateStatus)q.getSingleResult();
		} catch (NoResultException e) {
			st = new SmugmugUpdateStatus(handle);
			st.setAlbumHash("0");
			em.persist(st);
			//throw new NotFoundException("Have not cached a status for " + " handle: " + handle);
		}
		logger.debug("Smugmug update status exists for user " + handle);
		return st;
	}

	@Override
	protected Class<? extends CachedExternalUpdater<SmugmugUpdateStatus>> getUpdater() {
		return SmugmugUpdater.class;
	}

	@Override
	protected PollingTaskFamilyType getTaskFamily() {
		return PollingTaskFamilyType.SMUGMUG;
	}

	private static class SmugmugTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicitySeconds() {
			return 1 * 60; // 1 minutes
		}

		public String getName() {
			return PollingTaskFamilyType.SMUGMUG.name();
		}

		public long rescheduleSeconds(long suggestedSeconds) {
			return suggestedSeconds;
		}
	}
	
	private static PollingTaskFamily family = new SmugmugTaskFamily();
	
	private static class SmugmugTask extends PollingTask {
		private String username;
		
		public SmugmugTask(String username) {
			this.username = username;
		}

		@Override
		protected PollResult execute() throws Exception {
			SmugmugAlbumsCache cache = CacheFactoryBean.defaultLookup(SmugmugAlbumsCache.class);
			
			final List<? extends Image> albums = cache.getSync(username, true);
			
			return TxUtils.runInTransaction(new Callable<PollResult>() {
				public PollResult call() {
					DataService.getModel().initializeReadWriteSession(SystemViewpoint.getInstance());
					
					SmugmugUpdater proxy = EJBUtil.defaultLookup(SmugmugUpdater.class);
					boolean changed = proxy.saveUpdatedStatus(username, albums);
					
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
		return new SmugmugTask(handle);
	}
}
