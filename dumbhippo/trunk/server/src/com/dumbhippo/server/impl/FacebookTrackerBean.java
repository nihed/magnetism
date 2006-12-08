package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.CachedFacebookPhotoData;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookAlbumData;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoDataStatus;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.services.FacebookWebServices;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.ExpiredCacheException;
import com.dumbhippo.services.caches.FacebookPhotoDataCache;
import com.dumbhippo.services.caches.NotCachedException;
import com.dumbhippo.services.caches.WebServiceCache;

@Stateless
public class FacebookTrackerBean implements FacebookTracker {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FacebookTrackerBean.class);
	
	// how long to wait on the Facebook API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
		
	@EJB
	private Configuration config;
	
	@EJB
	private Notifier notifier;
	
	@EJB
	private TransactionRunner runner;

	@WebServiceCache
	private FacebookPhotoDataCache taggedPhotosCache;
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken) {
		ExternalAccount externalAccount = externalAccounts.getOrCreateExternalAccount(viewpoint, ExternalAccountType.FACEBOOK);
		
		FacebookAccount facebookAccount;
		if (externalAccount.getExtra() == null) {
		    facebookAccount = new FacebookAccount(externalAccount);
		    em.persist(facebookAccount);
		    externalAccount.setExtra(Long.toString(facebookAccount.getId()));
		    externalAccounts.setSentiment(externalAccount, Sentiment.LOVE);
		} else {
			facebookAccount = em.find(FacebookAccount.class, Long.parseLong(externalAccount.getExtra()));
			if (facebookAccount == null)
				throw new RuntimeException("Invalid FacebookAccount id " + externalAccount.getExtra() + " is stored in externalAccount " + externalAccount);
		}
		
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		ws.updateSession(facebookAccount, facebookAuthToken);
		
		long updateTime = (new Date()).getTime();
		long time = ws.updateMessageCount(facebookAccount);
		if (time != -1) {
		    updateTime = time;
		} else {
			// we also want to make sure the message count timestamp is refreshed
			facebookAccount.setMessageCountTimestampAsLong(updateTime);
		}
		FacebookEvent loginStatusEvent = getLoginStatusEvent(facebookAccount, true);
		if (loginStatusEvent != null)  
		    notifier.onFacebookEvent(facebookAccount.getExternalAccount().getAccount().getOwner(), loginStatusEvent);
	}

	// FIXME this is calling web services with a transaction open, which holds 
	// a db connection open so other threads can't use it, and could also 
	// time out the transaction
	public void updateMessageCount(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);

		// we could do these requests in parallel, but be careful about updating the same facebookAccount
		long time = ws.updateMessageCount(facebookAccount);
		if (time != -1) {
			// we recycle the event about unread messages for a given facebook account
			FacebookEvent unreadMessagesEvent = facebookAccount.getRecyclableEvent(FacebookEventType.UNREAD_MESSAGES_UPDATE);
			if (unreadMessagesEvent == null) {
				unreadMessagesEvent = 
					new FacebookEvent(facebookAccount, FacebookEventType.UNREAD_MESSAGES_UPDATE, 
	                                  facebookAccount.getUnreadMessageCount(), facebookAccount.getMessageCountTimestampAsLong());
				persistFacebookEvent(unreadMessagesEvent);
			} else {
				unreadMessagesEvent.setCount(facebookAccount.getUnreadMessageCount());
				unreadMessagesEvent.setEventTimestampAsLong(facebookAccount.getMessageCountTimestampAsLong());
			}
			notifier.onFacebookEvent(user, unreadMessagesEvent);
		}
		if (facebookAccount.isSessionKeyValid()) {
		    FacebookEvent newWallMessagesEvent = ws.updateWallMessageCount(facebookAccount);
		    if (newWallMessagesEvent != null) {
		    	// we create an individual event for each wall messages update
				persistFacebookEvent(newWallMessagesEvent);
		    	notifier.onFacebookEvent(user, newWallMessagesEvent);		    	
		    }
		    
		    if (facebookAccount.isSessionKeyValid()) {
		    	long pokeTime = ws.updatePokeCount(facebookAccount);
		    	if (pokeTime != -1) {
					// we recycle the event about unseen pokes for a given facebook account
					FacebookEvent unseenPokesEvent = facebookAccount.getRecyclableEvent(FacebookEventType.UNSEEN_POKES_UPDATE);
					if (unseenPokesEvent == null) {
						unseenPokesEvent = 
							new FacebookEvent(facebookAccount, FacebookEventType.UNSEEN_POKES_UPDATE, 
			                                  facebookAccount.getUnseenPokeCount(), facebookAccount.getPokeCountTimestampAsLong());
						persistFacebookEvent(unseenPokesEvent);
					} else {
						unseenPokesEvent.setCount(facebookAccount.getUnseenPokeCount());
						unseenPokesEvent.setEventTimestampAsLong(facebookAccount.getPokeCountTimestampAsLong());
					}
					notifier.onFacebookEvent(user, unseenPokesEvent);
		    	}
		    }
		} else {
			notifier.onFacebookEvent(user, getLoginStatusEvent(facebookAccount, false));
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void updateTaggedPhotos(final long facebookAccountId) {
		Pair <FacebookAccount, Integer> facebookAccountPair;
		try {
			facebookAccountPair = runner.runTaskInNewTransaction(new Callable<Pair<FacebookAccount, Integer>>() {
				public Pair <FacebookAccount, Integer> call() {
					FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);		
					// the photos are loaded lazily, so we need to get their count up-front
					return new Pair<FacebookAccount, Integer>(facebookAccount, facebookAccount.getTaggedPhotos().size());
				}	
			});
		} catch (Exception e) {
			logger.error("Failed to find facebookAccount for " + facebookAccountId, e.getMessage());
			throw new RuntimeException(e);
		}
			
		FacebookAccount facebookAccount = facebookAccountPair.getFirst();
		int oldPhotoCount = facebookAccountPair.getSecond();
		
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateTaggedPhotos()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		
		int newPhotoCount = ws.getTaggedPhotosCount(facebookAccount);
			
		FacebookTracker proxy = EJBUtil.defaultLookup(FacebookTracker.class);
		if (newPhotoCount == -1) {
			if (!facebookAccount.isSessionKeyValid()) {
				// that was a detached copy of facebookAccount, we need to set isSessionKeyValid on an attached copy
				proxy.handleExpiredSessionKey(facebookAccountId);
		    }
			return;			
		}
	
		// we will catch a NotCachedException if we don't have anything in cache (we never had it or had to delete it),
		// we will catch its subtype ExpiredCacheException if what we have in cache is expired (either we deliberately 
		// expired it because we expect an update or it naturally expired because it's old)
		boolean needCacheUpdate = false;       
		boolean expiredCache = false;		
		try {
		    taggedPhotosCache.checkCache(user.getGuid().toString());
		} catch (ExpiredCacheException e) {
			expiredCache = true;
			needCacheUpdate = true;
		} catch (NotCachedException e) {
			needCacheUpdate = true;
		}
	
		if (needCacheUpdate || (newPhotoCount != oldPhotoCount)) {
			if (expiredCache) {
				// with facebook, if the cache is expired we need to delete it
				taggedPhotosCache.deleteCache(user.getGuid().toString());
			} else if (newPhotoCount != oldPhotoCount) {
				// we just hope we will be able to get all the photos back right away
				// and not be hit by the cache that we ourselves expired on the next
				// iteration and have to delete it; what we really need to distinguish
				// these two situations is a needsRefresh flag on the cached data
				taggedPhotosCache.expireCache(user.getGuid().toString());
			}
			
//			 FIXME CachedFacebookPhotoData should not be leaking out of its cache bean
			List<? extends CachedFacebookPhotoData> cachedPhotos = taggedPhotosCache.getSync(user.getGuid().toString());
			
			proxy.saveUpdatedTaggedPhotos(facebookAccountId, cachedPhotos);
		}	
	}
	
	// cachedPhotos that we get here are not attached
	public void saveUpdatedTaggedPhotos(long facebookAccountId, List<? extends CachedFacebookPhotoData> cachedPhotos) { 
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);	

		if (!facebookAccount.getTaggedPhotosPrimed() && cachedPhotos.isEmpty()) {
			// this covers the case when the user did not have any photos tagged with them on facebook prior 
			// to adding their facebook account to mugshot
			facebookAccount.setTaggedPhotosPrimed(true);
			return;
		}
			
		Set<FacebookPhotoDataStatus> oldPhotos = facebookAccount.getTaggedPhotos();
		Set<FacebookPhotoDataStatus> oldPhotosToRemove = new HashSet<FacebookPhotoDataStatus>(oldPhotos);
		List<CachedFacebookPhotoData> newPhotosToAdd = new ArrayList<CachedFacebookPhotoData>(cachedPhotos);
		
		for (FacebookPhotoDataStatus oldPhoto : oldPhotos) {
			CachedFacebookPhotoData foundPhoto = null;
			for (CachedFacebookPhotoData newPhoto : newPhotosToAdd) {
				if (oldPhoto.matchPhotoId(newPhoto.getPhotoId())) {
					CachedFacebookPhotoData attachedCachedPhoto = em.find(CachedFacebookPhotoData.class, newPhoto.getId());
					oldPhoto.setNewPhotoData(attachedCachedPhoto);					
					oldPhotosToRemove.remove(oldPhoto);
					foundPhoto = newPhoto;
					break;
				}
			}
			if (foundPhoto != null)
			    newPhotosToAdd.remove(foundPhoto);
		}
		
		for (FacebookPhotoDataStatus oldPhotoToRemove : oldPhotosToRemove) {
			// deleting these photos would make us restack them if the web service glitched and
			// only returned a suset of photos at some point, let's rather keep the photos
			// with the ids we did not get, but make sure that the cached photos they point to are null
            if (oldPhotoToRemove.getPhotoData() != null) {
            	logger.warn("The cached photo data that was no longer returned for the facebookAccount was" +
            			    " still set on the FacebookPhotoDataStatus {}", oldPhotoToRemove);
            	oldPhotoToRemove.setNewPhotoData(null);
            }
		}
		
		if (newPhotosToAdd.size() > 0) {
			if (!facebookAccount.getTaggedPhotosPrimed()) {
				// this covers the case when the user has some photos tagged with them on facebook prior to adding
				// their facebook account to mugshot
			    for (CachedFacebookPhotoData newPhotoToAdd : newPhotosToAdd) {
			    	CachedFacebookPhotoData attachedCachedPhoto = em.find(CachedFacebookPhotoData.class, newPhotoToAdd.getId());
			    	FacebookPhotoDataStatus photoDataStatus = new FacebookPhotoDataStatus(facebookAccount, attachedCachedPhoto);
				    em.persist(photoDataStatus);
				    facebookAccount.addTaggedPhoto(photoDataStatus);
			    }
				facebookAccount.setTaggedPhotosPrimed(true);
				return;
			}
				
		    FacebookEvent taggedPhotosEvent = new FacebookEvent(facebookAccount, FacebookEventType.NEW_TAGGED_PHOTOS_EVENT, 
                                                                newPhotosToAdd.size(), (new Date()).getTime());		
			persistFacebookEvent(taggedPhotosEvent);
		    for (CachedFacebookPhotoData newPhotoToAdd : newPhotosToAdd) {
		    	CachedFacebookPhotoData attachedCachedPhoto = em.find(CachedFacebookPhotoData.class, newPhotoToAdd.getId());
		    	FacebookPhotoDataStatus photoDataStatus = new FacebookPhotoDataStatus(facebookAccount, attachedCachedPhoto);
		    	photoDataStatus.setFacebookEvent(taggedPhotosEvent);
			    em.persist(photoDataStatus);
			    taggedPhotosEvent.addPhoto(photoDataStatus);
			    facebookAccount.addTaggedPhoto(photoDataStatus);
		    }
		 
		    notifier.onFacebookEvent(facebookAccount.getExternalAccount().getAccount().getOwner(), taggedPhotosEvent);
		}
	}

	public void removeExpiredTaggedPhotos(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);	
					
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to removeExpiredTaggedPhotos()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		
		// we will catch a NotCachedException if we don't have anything in cache (we never had it or had to delete it),
		// we will catch its subtype ExpiredCacheException if what we have in cache is expired (either we deliberately 
		// expired it because we expect an update or it naturally expired because it's old)   
		boolean expiredCache = false;		
		try {
		    taggedPhotosCache.checkCache(user.getGuid().toString());
		} catch (ExpiredCacheException e) {
			expiredCache = true;
		} catch (NotCachedException e) {
			// nothing to do
		}
		
		if (expiredCache) {
			// we can't keep 'em anymore...
			taggedPhotosCache.deleteCache(user.getGuid().toString());
		}				
	}
	
	// FIXME this is calling web services with a transaction open, which holds 
	// a db connection open so other threads can't use it, and could also 
	// time out the transaction
	public void updateAlbums(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateAlbums()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		Set<FacebookAlbumData> modifiedAlbums = ws.getModifiedAlbums(facebookAccount);
		
		if (modifiedAlbums.isEmpty()) {
			if (!facebookAccount.isSessionKeyValid()) {
				notifier.onFacebookEvent(user, getLoginStatusEvent(facebookAccount, false));
		    }
			return;
		}
		
		Set<FacebookAlbumData> oldAlbums = facebookAccount.getAlbums();
		// modifiedAlbums are either albums that have been modified or completely new albums
		Set<FacebookAlbumData> newAlbumsToAdd = new HashSet<FacebookAlbumData>(modifiedAlbums);
		long albumsModifiedTimestamp = facebookAccount.getAlbumsModifiedTimestampAsLong();
		long updateTime = (new Date()).getTime();
		
		for (FacebookAlbumData oldAlbum : oldAlbums) {
			FacebookAlbumData foundAlbum = null;
			for (FacebookAlbumData newAlbum : newAlbumsToAdd) {
				if (newAlbum.getAlbumId().equals(oldAlbum.getAlbumId())) {
					oldAlbum.updateCachedData(newAlbum);
					// each time the album was modified we update the corresponding event timestamp,
					// we also make sure that its type is set to MODIFIED_ALBUM_EVENT
					oldAlbum.getFacebookEvent().setEventTimestampAsLong(updateTime);
					oldAlbum.getFacebookEvent().setEventType(FacebookEventType.MODIFIED_ALBUM_EVENT);
					if (albumsModifiedTimestamp < oldAlbum.getModifiedTimestampAsLong()) {
						albumsModifiedTimestamp = oldAlbum.getModifiedTimestampAsLong();
					}
					foundAlbum = newAlbum;
					notifier.onFacebookEvent(user, oldAlbum.getFacebookEvent());	
					break;
				}
			}
			if (foundAlbum != null)
			    newAlbumsToAdd.remove(foundAlbum);
		}
		
		for (FacebookAlbumData newAlbumToAdd : newAlbumsToAdd) {
		    em.persist(newAlbumToAdd.getCoverPhoto());
		    em.persist(newAlbumToAdd);
		    facebookAccount.addAlbum(newAlbumToAdd);

	    	FacebookEvent newAlbumEvent = new FacebookEvent(facebookAccount, FacebookEventType.NEW_ALBUM_EVENT,
	    			                                             1, updateTime);
	    	newAlbumEvent.setAlbum(newAlbumToAdd);
			persistFacebookEvent(newAlbumEvent);
	    	newAlbumToAdd.setFacebookEvent(newAlbumEvent);
	    	// albumsModifiedTimestamp on facebookAccount will be less than 0 if this is the first
	    	// time we are uploading information about the user's albums, in which case we do not 
	    	// want to stack a new album event 
	    	if (facebookAccount.getAlbumsModifiedTimestampAsLong() > 0) {
	    		notifier.onFacebookEvent(user, newAlbumEvent);
	    	}

			if (albumsModifiedTimestamp < newAlbumToAdd.getModifiedTimestampAsLong()) {
				albumsModifiedTimestamp = newAlbumToAdd.getModifiedTimestampAsLong();
			}		    
		}
		
		facebookAccount.setAlbumsModifiedTimestampAsLong(albumsModifiedTimestamp);		
	}

	public void handleExpiredSessionKey(long facebookAccountId) {
		FacebookAccount attachedFacebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		attachedFacebookAccount.setSessionKeyValid(false);
		User user = attachedFacebookAccount.getExternalAccount().getAccount().getOwner();
		notifier.onFacebookEvent(user, getLoginStatusEvent(attachedFacebookAccount, false));	
	}
	
	private FacebookEvent getLoginStatusEvent(FacebookAccount facebookAccount, boolean signedIn) {
		FacebookEvent loginStatusEvent = facebookAccount.getRecyclableEvent(FacebookEventType.LOGIN_STATUS_EVENT);
		if (loginStatusEvent == null) {
			loginStatusEvent = 
				new FacebookEvent(facebookAccount, FacebookEventType.LOGIN_STATUS_EVENT, 
	                              signedIn ? 1 : 0, (new Date()).getTime());
			persistFacebookEvent(loginStatusEvent);
		} else {
			// we should not re-stack the login status if the person was already logged in
			if (signedIn && (loginStatusEvent.getCount() == 1))
				return null;			
			loginStatusEvent.setCount(signedIn ? 1 : 0);
			loginStatusEvent.setEventTimestampAsLong((new Date()).getTime());
		}		
		
		return loginStatusEvent; 
	}
	
	private void persistFacebookEvent(FacebookEvent event) {
        em.persist(event);
        event.getFacebookAccount().addFacebookEvent(event);
        notifier.onFacebookEventCreated(event.getFacebookAccount().getExternalAccount().getAccount().getOwner(),
        		                        event);
	}
}
