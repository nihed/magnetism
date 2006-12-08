package com.dumbhippo.server.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTaskFamily;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.FlickrUpdateStatus;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.FlickrUpdater;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotosView;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.FlickrPhotosetsView;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.FlickrPhotosetPhotosCache;
import com.dumbhippo.services.caches.FlickrUserPhotosCache;
import com.dumbhippo.services.caches.FlickrUserPhotosetsCache;
import com.dumbhippo.services.caches.WebServiceCache;

@Stateless
public class FlickrUpdaterBean extends CachedExternalUpdaterBean<FlickrUpdateStatus> implements FlickrUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FlickrUpdaterBean.class);		
	
	@EJB
	private Notifier notifier;

	@WebServiceCache
	private FlickrUserPhotosCache userPhotosCache;
	
	@WebServiceCache
	private FlickrUserPhotosetsCache userPhotosetsCache;
	
	@WebServiceCache
	private FlickrPhotosetPhotosCache photosetPhotosCache;

	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	@Override
	public Query getCachedStatusQuery(String flickrId) {
		Query q = em.createQuery("SELECT updateStatus FROM FlickrUpdateStatus updateStatus " +
				"WHERE updateStatus.flickrId = :flickrId");
		q.setParameter("flickrId", flickrId);
		return q;
	}
	
	public Set<String> getActiveFlickrUserIds() {
		return getActiveUsers();
	}

	public Collection<User> getUsersWhoLoveFlickrAccount(String flickrUserId) {
		return getAccountLovers(flickrUserId);
	}
	
	public Collection<FlickrPhotosetStatus> getPhotosetStatusesForFlickrAccount(String flickrUserId) {
		Query q = em.createQuery("SELECT photosetStatus FROM FlickrPhotosetStatus photosetStatus WHERE " + 
				" photosetStatus.ownerId = :ownerId");
		q.setParameter("ownerId", flickrUserId);
		return TypeUtils.castList(FlickrPhotosetStatus.class, q.getResultList());
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)	
	public void doPeriodicUpdate(String flickrId) {
		FlickrUpdater proxy = EJBUtil.defaultLookup(FlickrUpdater.class);

		FlickrPhotosView photosView = userPhotosCache.getSync(flickrId, true);
		FlickrPhotosetsView photosetsView = userPhotosetsCache.getSync(flickrId, true);
		
		if (photosView == null || photosetsView == null) {
			logger.debug("one of two flickr requests failed, not saving new flickr status for " + flickrId);
			return;
		}
		
		proxy.saveUpdatedStatus(flickrId, photosView, photosetsView);
	}
	
	private void updateUserPhotosetStatuses(String ownerId, List<? extends FlickrPhotosetView> allPhotosets) {
		Query q = em.createQuery("SELECT setStatus FROM FlickrPhotosetStatus setStatus WHERE " + 
				"setStatus.ownerId = :ownerId");
		q.setParameter("ownerId", ownerId);
		List<FlickrPhotosetStatus> statuses = TypeUtils.castList(FlickrPhotosetStatus.class, q.getResultList());
		Map<String,FlickrPhotosetView> viewsBySetId = new HashMap<String,FlickrPhotosetView>();
		for (FlickrPhotosetView view : allPhotosets) {
			viewsBySetId.put(view.getId(), view);
		}
		
		// for each view we already have a status for, see if it changed
		for (FlickrPhotosetStatus status : statuses) {
			FlickrPhotosetView view = viewsBySetId.get(status.getFlickrId());
			viewsBySetId.remove(status.getFlickrId());
			// view may be null, then update() will status.setActive(false)
			if (status.update(view)) {
				logger.debug("flickr photoset status changed, now {}", status);
				
				notifier.onFlickrPhotosetChanged(status);
			}
		}
		
		// remaining views require creating a new status object
		for (FlickrPhotosetView view : viewsBySetId.values()) {
			FlickrPhotosetStatus status = new FlickrPhotosetStatus(ownerId, view.getId());
			status.update(view);
			em.persist(status);
			
			logger.debug("created new flickr photoset status {}", status);
			
			notifier.onFlickrPhotosetCreated(status);
		}
	}
	
	// compute a "hash" (relies on the most recent photos being first, 
	// so it always changes if there are new photos)
	private String computePhotosHash(List<? extends FlickrPhotoView> photoViews) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= photoViews.size())
				break;
			sb.append(photoViews.get(i).getId());
			sb.append("-"); // just leave it trailing on the last one, doesn't matter
		}
		return sb.toString();
	}
	
	// compute a "hash" (relies on the most recent photos being first, 
	// so it always changes if there are new photos)
	private String computePhotosetsHash(List<? extends FlickrPhotosetView> photosetViews) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= photosetViews.size())
				break;
			sb.append(photosetViews.get(i).getId());
			sb.append("-"); // just leave it trailing on the last one, doesn't matter
		}
		return sb.toString();
	}
	
	// Our job is to notify so blocks can be created/stacked, and 
	// to save the new status in the FlickrUpdateStatus table.
	// We do this in one huge transaction.
	public boolean saveUpdatedStatus(String flickrId, FlickrPhotosView photosView, FlickrPhotosetsView photosetsView) { 
		if (photosView == null)
			throw new IllegalArgumentException("null photosView");
		if (photosetsView == null)
			throw new IllegalArgumentException("null photosetsView");

		logger.debug("Saving new flickr status for " + flickrId + ": photos {} sets {}",
				photosView.getTotal(), photosetsView.getTotal());
		
		FlickrUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(flickrId);
		} catch (NotFoundException e) {
			updateStatus = new FlickrUpdateStatus(flickrId);
			em.persist(updateStatus);
		}
		
		String photosHash = computePhotosHash(photosView.getPhotos());
		String photosetsHash = computePhotosetsHash(photosetsView.getSets());
		
		boolean photosChanged = false;
		boolean photosetsChanged = false;
		
		if (!updateStatus.getMostRecentPhotos().equals(photosHash)) {
			logger.debug("Most recent photos changed '{}' -> '{}'",
					updateStatus.getMostRecentPhotos(), photosHash);
			photosChanged = true;
			updateStatus.setMostRecentPhotos(photosHash);
		}
		
		if (!updateStatus.getMostRecentPhotosets().equals(photosetsHash)) {
			logger.debug("Most recent photosets changed '{}' -> '{}'",
					updateStatus.getMostRecentPhotosets(), photosetsHash);
			photosetsChanged = true;
			updateStatus.setMostRecentPhotosets(photosetsHash);
		}
		
		// Push the updateStatus object to the database. The theory here is that 
		// it keeps Hibernate from reordering saving the new UpdateStatus vs. 
		// the other work we're about to do below, which might help avoid 
		// deadlock situations with the 2nd level cache. 
		// (If two threads both do A,B that's safer than having one do A,B and
		// one B,A.) Since we're done with the updateStatus
		// at this point, it shouldn't hurt efficiency too much to do this.
		em.flush();
		
		if (photosChanged || photosetsChanged) {
			// If anything changes, drop all the cached photoset contents
			// to be sure we display all photoset blocks correctly. This
			// is serious overkill, but avoids a lot of complexity with 
			// tracking changes to each photoset's photo list individually.
			// However, this approach does not let us restack a photoset
			// block if the photoset contents change; to do that we'd need
			// to track a "hash" of the most recent photos in the set in
			// FlickrPhotosetStatus just as we do for people's total list 
			// of photos.
			for (FlickrPhotosetView photoset : photosetsView.getSets()) {
				photosetPhotosCache.expireCache(photoset.getId());				
			}
			// Again, superstitious predictable ordering (see above)
			em.flush();
		}

		// Try to do the notifications after we already did the earlier stuff.
		// This will stack some blocks, and create/modify FlickrPhotosetStatus objects
		
		if (photosChanged)
			notifier.onMostRecentFlickrPhotosChanged(flickrId, photosView);
		
		// Again, superstitious predictable ordering (see above)
		em.flush();
		
		if (photosetsChanged)
			updateUserPhotosetStatuses(flickrId, photosetsView.getSets());
		return photosChanged || photosetsChanged;
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.FLICKR;
	}

	@Override
	protected Class<? extends CachedExternalUpdater<FlickrUpdateStatus>> getUpdater() {
		return FlickrUpdater.class;
	}

	@Override
	protected PollingTaskFamilyType getTaskFamily() {
		return PollingTaskFamilyType.FLICKR;
	}

	private static class FlickrTaskFamily implements PollingTaskFamily {

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
			return PollingTaskFamilyType.FLICKR.name();
		}
	}
	
	private static PollingTaskFamily family = new FlickrTaskFamily();
	
	private static class FlickrTask extends PollingTask {
		private String flickrId;
		
		public FlickrTask(String username) {
			this.flickrId = username;
		}

		@Override
		protected PollResult execute() throws Exception {
			boolean changed = false;
			FlickrUpdater proxy = EJBUtil.defaultLookup(FlickrUpdater.class);
			FlickrUserPhotosCache userPhotosCache = EJBUtil.defaultLookup(FlickrUserPhotosCache.class);
			FlickrUserPhotosetsCache userPhotosetsCache = EJBUtil.defaultLookup(FlickrUserPhotosetsCache.class);
			
			FlickrPhotosView photosView = userPhotosCache.getSync(flickrId, true);
			FlickrPhotosetsView photosetsView = userPhotosetsCache.getSync(flickrId, true);
			
			changed = proxy.saveUpdatedStatus(flickrId, photosView, photosetsView); 
			return new PollResult(changed, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return flickrId;
		}
	}

	@Override
	protected PollingTask createPollingTask(String handle) {
		return new FlickrTask(handle);
	}		
}
