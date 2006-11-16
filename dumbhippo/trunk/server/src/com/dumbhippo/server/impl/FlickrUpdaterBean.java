package com.dumbhippo.server.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.FlickrUpdateStatus;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.services.caches.FlickrPhotosetPhotosCache;
import com.dumbhippo.server.FlickrUpdater;
import com.dumbhippo.services.caches.FlickrUserPhotosCache;
import com.dumbhippo.services.caches.FlickrUserPhotosetsCache;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.FlickrPhotosets;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class FlickrUpdaterBean implements FlickrUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FlickrUpdaterBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 	
	
	@EJB
	private Configuration config;
	
	@EJB
	private FlickrUserPhotosCache userPhotosCache;
	
	@EJB
	private FlickrUserPhotosetsCache userPhotosetsCache;
	
	@EJB
	private FlickrPhotosetPhotosCache photosetPhotosCache;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private Notifier notifier;
	
	public FlickrUpdateStatus getCachedStatus(User user) throws NotFoundException {
		ExternalAccount external = user.getAccount().getExternalAccount(ExternalAccountType.FLICKR);
		if (external == null)
			throw new NotFoundException("User has no flickr external account: " + user);
		// sentiment and handle are checked in here
		return getCachedStatus(external);
	}
	
	public FlickrUpdateStatus getCachedStatus(ExternalAccount flickrAccount) throws NotFoundException {
		if (flickrAccount.getAccountType() != ExternalAccountType.FLICKR)
			throw new RuntimeException("Invalid account type passed to FlickrUpdater " + flickrAccount);
		
		if (flickrAccount.getHandle() == null || 
				flickrAccount.getSentiment() != Sentiment.LOVE)
			throw new NotFoundException("Flickr account has null handle or is unloved: " + flickrAccount);
		return getCachedStatus(flickrAccount.getHandle());
	}
	
	public FlickrUpdateStatus getCachedStatus(String flickrId) throws NotFoundException {
		Query q = em.createQuery("SELECT updateStatus FROM FlickrUpdateStatus updateStatus " +
				"WHERE updateStatus.flickrId = :flickrId");
		q.setParameter("flickrId", flickrId);
		
		try {
			return (FlickrUpdateStatus) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("Have not cached a status for flickr id: " + flickrId);
		}
	}
	
	public Set<String> getActiveFlickrUserIds() {
		Query q = em.createQuery("SELECT ea.handle FROM ExternalAccount ea WHERE " + 
				" ea.accountType = " + ExternalAccountType.FLICKR.ordinal() + 
				" AND ea.sentiment = " + Sentiment.LOVE.ordinal() + 
				" AND ea.handle IS NOT NULL " + 
				" AND ea.account.disabled = false AND ea.account.adminDisabled = false");
		// we need a set because multiple accounts can have the same flickr account
		Set<String> results = new HashSet<String>();
		results.addAll(TypeUtils.castList(String.class, q.getResultList()));
		return results;
	}

	public Collection<User> getUsersWhoLoveFlickrAccount(String flickrUserId) {
		Query q = em.createQuery("SELECT ea.account.owner FROM ExternalAccount ea WHERE " +
				"  ea.accountType = " + ExternalAccountType.FLICKR.ordinal() + 
				"  AND ea.sentiment = " + Sentiment.LOVE.ordinal() + 
				"  AND ea.handle = :flickrUserId " + 
				"  AND ea.account.disabled = false AND ea.account.adminDisabled = false");
		q.setParameter("flickrUserId", flickrUserId);
		return TypeUtils.castList(User.class, q.getResultList());
	}
	
	public Collection<FlickrPhotosetStatus> getPhotosetStatusesForFlickrAccount(String flickrUserId) {
		Query q = em.createQuery("SELECT photosetStatus FROM FlickrPhotosetStatus photosetStatus WHERE " + 
				" photosetStatus.ownerId = :ownerId");
		q.setParameter("ownerId", flickrUserId);
		return TypeUtils.castList(FlickrPhotosetStatus.class, q.getResultList());
	}
	
	// avoid log messages in here that will happen on every call, or it could get noisy
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void periodicUpdate(String flickrId) {
		EJBUtil.assertNoTransaction();
		
		FlickrWebServices ws = new FlickrWebServices(5000, config);
		
		// These do NOT use any cache - remember, we're polling periodically for changes.
		FlickrPhotos photos = ws.lookupPublicPhotos(flickrId, 1, FlickrWebServices.MIN_PER_PAGE);
		FlickrPhotosets photosets = null;
		//	no sets possible if no photos
		if (photos != null && photos.getTotal() > 0) {
			// unfortunately, it isn't obvious how we can get the photoset count 
			// without getting all the sets
			photosets = ws.lookupPublicPhotosets(flickrId);
		}
	
		int totalPhotos = photos != null ? photos.getTotal() : 0;
		int totalPhotosets = photosets != null ? photosets.getSets().size() : 0;

		boolean changesLikely = false;
		FlickrUpdater proxy = EJBUtil.defaultLookup(FlickrUpdater.class);
		try {
			// The idea is to have only this one transaction if we have no work 
			// to do
			FlickrUpdateStatus detachedStatus = proxy.getCachedStatus(flickrId);
			if (detachedStatus.getTotalPhotoCount() != totalPhotos ||
				detachedStatus.getTotalPhotosetCount() != totalPhotosets) {
				changesLikely = true;
			}
		} catch (NotFoundException e) {
			// we have no idea about previous state
			changesLikely = true;
		}
		
		// If we now have work to do, we start creating all kinds of 
		// transactions, woot
		if (changesLikely) {
			// Expire the caches to force a reload. Note that 
			// we reload both caches if either count has changed;
			// that's because we're trying to get changes to photoset
			// content as well as changes to what photosets there are,
			// and also changes to titles, comments, etc. - so aggressively
			// expiring the cache improves our chances of seeing new stuff.
			userPhotosCache.expireCache(flickrId);
			userPhotosetsCache.expireCache(flickrId);
			
			// New web services calls. Note we already made these above,
			// but not through the cache, and with the least expense possible
			// (e.g. asking for only one photo). So we do it again here.
			// Flickr is giving us new total photo/photoset counts in these
			// calls but we're ignoring them, which could lead to extra work,
			// but too complex to avoid and basically harmless.
			// The list of photos is limited to only a few, the list of 
			// photo sets is supposed to be complete. This is because 
			// we have a block per photo set but only one block for all photos.
			List<FlickrPhotoView> photoViews = userPhotosCache.getSync(flickrId);
			List<FlickrPhotosetView> photosetViews = userPhotosetsCache.getSync(flickrId);
			
			proxy.saveUpdatedStatus(flickrId, totalPhotos, totalPhotosets,
					photoViews, photosetViews);
		}
	}
	
	private void updateUserPhotosetStatuses(String ownerId, List<FlickrPhotosetView> allPhotosets) {
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
	private String computePhotosHash(List<FlickrPhotoView> photoViews) {
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
	private String computePhotosetsHash(List<FlickrPhotosetView> photosetViews) {
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
	public void saveUpdatedStatus(String flickrId, int totalPhotos, int totalPhotosets,
			List<FlickrPhotoView> photoViews, List<FlickrPhotosetView> photosetViews) {
		logger.debug("Saving new flickr status for " + flickrId + ": photos {} sets {}",
				totalPhotos, totalPhotosets);
		
		FlickrUpdateStatus updateStatus;
		try {
			updateStatus = getCachedStatus(flickrId);
		} catch (NotFoundException e) {
			updateStatus = new FlickrUpdateStatus(flickrId);
			em.persist(updateStatus);
		}
		updateStatus.setTotalPhotoCount(totalPhotos);
		updateStatus.setTotalPhotosetCount(totalPhotosets);
		
		String photosHash = computePhotosHash(photoViews);
		String photosetsHash = computePhotosetsHash(photosetViews);
		
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
			for (FlickrPhotosetView photoset : photosetViews) {
				photosetPhotosCache.expireCache(photoset.getId());				
			}
			// Again, superstitious predictable ordering (see above)
			em.flush();
		}

		// Try to do the notifications after we already did the earlier stuff.
		// This will stack some blocks, and create/modify FlickrPhotosetStatus objects
		
		if (photosChanged)
			notifier.onMostRecentFlickrPhotosChanged(flickrId, photoViews);
		
		// Again, superstitious predictable ordering (see above)
		em.flush();
		
		if (photosetsChanged)
			updateUserPhotosetStatuses(flickrId, photosetViews);
	} 

	private boolean isLovedAndEnabledFlickr(ExternalAccount external) {
		return external.hasLovedAndEnabledType(ExternalAccountType.FLICKR) && 
			external.getHandle() != null;
	}
	
	private void doPeriodicUpdateNow(ExternalAccount external) {
		// Immediately update the flickr account status instead of 
		// waiting for the next periodic update to kick in, so if someone
		// adds a flickr account they can see the result right away.
		
		final String flickrId = external.getHandle();
		runner.runTaskOnTransactionCommit(new Runnable() {

			public void run() {
				FlickrUpdater updater = EJBUtil.defaultLookup(FlickrUpdater.class);
				updater.periodicUpdate(flickrId);
			}
			
		});		
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		if (!isLovedAndEnabledFlickr(external))
			return;
		
		doPeriodicUpdateNow(external);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (!isLovedAndEnabledFlickr(external))
			return;

		doPeriodicUpdateNow(external);
	}
}
