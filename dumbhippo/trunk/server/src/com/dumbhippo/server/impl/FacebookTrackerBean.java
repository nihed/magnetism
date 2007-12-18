package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.Site;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookAlbumData;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoDataStatus;
import com.dumbhippo.persistence.FacebookResource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.FacebookSystemException;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.services.FacebookPhotoDataView;
import com.dumbhippo.services.FacebookWebServices;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.ExpiredCacheException;
import com.dumbhippo.services.caches.FacebookPhotoDataCache;
import com.dumbhippo.services.caches.NotCachedException;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class FacebookTrackerBean implements FacebookTracker {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FacebookTrackerBean.class);
	
	// how long to wait on the Facebook API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	// let's get 6, so that we know if there is more when displaying 5
	static private final int INITIAL_BLOCKS_PER_PAGE = 6;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
	
	@EJB
	private Configuration config;
	
	@EJB
	private Notifier notifier;
	
	@EJB
	@IgnoreDependency
	private Stacker stacker;
	
	@WebServiceCache
	private FacebookPhotoDataCache taggedPhotosCache;
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@EJB
	private FacebookSystem facebookSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private AccountSystem accounts;
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken) throws FacebookSystemException {
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		Pair<String, String> facebookInfo = ws.getSession(facebookAuthToken);
		String sessionKey = facebookInfo.getFirst();
		String facebookUserId = facebookInfo.getSecond();
		
		if (sessionKey == null || facebookUserId == null)
			return;
		
		updateOrCreateFacebookAccount(viewpoint, sessionKey, facebookUserId, false);
	}

	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String sessionKey, String facebookUserId, boolean applicationEnabled) throws FacebookSystemException {
		ExternalAccount externalAccount = externalAccounts.getOrCreateExternalAccount(viewpoint, ExternalAccountType.FACEBOOK);
		
		FacebookAccount facebookAccount;
		if (externalAccount.getExtra() == null) {
			FacebookResource res = null;
			try {
			    res = identitySpider.lookupFacebook(facebookUserId);
			} catch (NotFoundException e) {
				// nothing to do
			}
			facebookAccount = getFacebookAccount(facebookUserId);
			if (res == null && facebookAccount == null) {
				res = new FacebookResource(facebookUserId);
				em.persist(res);
				identitySpider.addVerifiedOwnershipClaim(viewpoint.getViewer(), res);
			    facebookAccount = new FacebookAccount(externalAccount, facebookUserId);
			    em.persist(facebookAccount);
			    externalAccount.setExtra(Long.toString(facebookAccount.getId()));		
			} else if (res != null && facebookAccount != null) {
				AccountClaim ac = res.getAccountClaim();
				if (ac != null) {
					if (!ac.getOwner().equals(viewpoint.getViewer())) {
						if (!ac.getOwner().getAccount().getHasAcceptedTerms()) {
							// The only way this could happen is if we created a temporary Facebook account based on the Facebook user id,
							// and now a user is verifying their Mugshot account from Facebook.
							// We need to remove sentiment 'love' for the Facebook external account from it, disable it, and remove
							// the claim of ownership on the Facebook resource, then we can add that Facebook information to the other 
							// person's account.
							// TODO: consider checking if the temporary account had any additional external accounts set that can be
							// added or moved to the account that is being verified.
							try {
							    ExternalAccount ea = externalAccounts.lookupExternalAccount(new UserViewpoint(ac.getOwner(), Site.MUGSHOT), ac.getOwner(), ExternalAccountType.FACEBOOK);
							    externalAccounts.setSentiment(ea, Sentiment.INDIFFERENT);
							    ac.getOwner().getAccount().setDisabled(true);
							    em.remove(ac);
							    em.flush();
							    identitySpider.addVerifiedOwnershipClaim(viewpoint.getViewer(), res);
							    externalAccount.setExtra(Long.toString(facebookAccount.getId()));		
							} catch (NotFoundException e) {
								throw new RuntimeException("We expected to find a Facebook external account for user " + ac.getOwner() + ", but we didn't!");
							}
						} else {
						    throw new FacebookSystemException("Facebook account " + facebookUserId + " is claimed by someone else: " + ac.getOwner());
						}
					} else {
						throw new RuntimeException("Facebook account " + facebookUserId + " is claimed by the user " + viewpoint.getViewer() + " whose ExternalAccount for Facebook doesn't reflect the claim");
					}
				} else {
					// we could also check here that there is no ExternalAccount of type Facebook with extra referencing 
					// this FacebookAccount
					assert(facebookAccount.getExternalAccount()== null);
					identitySpider.addVerifiedOwnershipClaim(viewpoint.getViewer(), res);					
				    externalAccount.setExtra(Long.toString(facebookAccount.getId()));	
				}
			} else if (res != null && facebookAccount == null) {
				// this can only happen if we are creating a new User and already had to set their FacebookResource
				AccountClaim ac = res.getAccountClaim();
				if (ac != null) {
					if (!ac.getOwner().equals(viewpoint.getViewer())) {
						throw new FacebookSystemException("Facebook account " + facebookUserId + " is claimed by someone else: " + ac.getOwner());
					}
				} else {
					logger.warn("Had a FacebookResource for " + facebookUserId + " with no account claim and no corresponding FacebookAccount");
					identitySpider.addVerifiedOwnershipClaim(viewpoint.getViewer(), res);	
				}
			    facebookAccount = new FacebookAccount(externalAccount, facebookUserId);
			    em.persist(facebookAccount);
			    externalAccount.setExtra(Long.toString(facebookAccount.getId()));					
			} else {
				throw new RuntimeException("Facebook resource was " + res + ", while Facebook account was " + facebookAccount + ". Every FacebookAccount should have a corresponding FacebookResource.");				
			}
		} else {
			facebookAccount = em.find(FacebookAccount.class, Long.parseLong(externalAccount.getExtra()));
			if (facebookAccount == null)
				throw new RuntimeException("Invalid FacebookAccount id " + externalAccount.getExtra() + " is stored in externalAccount " + externalAccount);
			if (!facebookAccount.getFacebookUserId().equals(facebookUserId)) {
				throw new FacebookSystemException("We do not support changing your Facebook account yet.");
			}
		    Query resourceQuery = em.createQuery("from FacebookResource f where f.facebookUserId = :facebookUserId");
			resourceQuery.setParameter("facebookUserId", facebookUserId);
			try {
				FacebookResource res = identitySpider.lookupFacebook(facebookUserId);
				assert(res.getAccountClaim().equals(viewpoint.getViewer()));
			} catch (NotFoundException e) {
				throw new RuntimeException("No FacebookResource found for " + facebookUserId + ", while there exists a corresponding FacebookAccount");
			}			
		}
		
	    facebookAccount.setSessionKey(sessionKey);
	    if (sessionKey != null)
		    facebookAccount.setSessionKeyValid(true);	
	    facebookAccount.setApplicationEnabled(applicationEnabled);
		// make sure the sentiment is LOVE; there is currently no way to unset it from the user interface,
		// but we should allow changing the sentiment to HATE or at least INDIFFERENT in the future
		externalAccounts.setSentiment(externalAccount, Sentiment.LOVE);
	    
		FacebookEvent loginStatusEvent = getLoginStatusEvent(facebookAccount, true);
		if (loginStatusEvent != null)  
		    notifier.onFacebookEvent(facebookAccount.getExternalAccount().getAccount().getOwner(), loginStatusEvent);
		
		if (applicationEnabled) {
			final User user = viewpoint.getViewer();
		    TxUtils.runOnCommit(new Runnable() {
			    public void run() {
			    	updateFbmlForUser(user);
			    }
		    });
		}
	}
	
	public User createNewUserWithFacebookAccount(String sessionKey, String facebookUserId, boolean applicationEnabled) throws FacebookSystemException {
		// the resource might have already existed, but not claimed by anyone
		FacebookResource res;
		try {
		    res = identitySpider.lookupFacebook(facebookUserId);
		    assert(res.getAccountClaim() == null);
		} catch (NotFoundException e) {
			res = new FacebookResource(facebookUserId);
			em.persist(res);
		}
		Account account = accounts.createAccountFromResource(res);
		User user = account.getOwner();
		updateOrCreateFacebookAccount(new UserViewpoint(user, Site.MUGSHOT), sessionKey, facebookUserId, applicationEnabled);
		return user;
	}
	
	public FacebookAccount getFacebookAccount(String facebookUserId) {
		Query accountQuery = em.createQuery("from FacebookAccount f where f.facebookUserId = :facebookUserId");
		accountQuery.setParameter("facebookUserId", facebookUserId);
		try {
			return (FacebookAccount)accountQuery.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}		
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void updateFbmlForUser(final User user) {
		TxUtils.assertNoTransaction();
		try {
			FacebookAccount facebookAccount = TxUtils.runInTransaction(new Callable<FacebookAccount>() {
				public FacebookAccount call() {
					try {
					    return facebookSystem.lookupFacebookAccount(SystemViewpoint.getInstance(), user);
					} catch (NotFoundException e) {
						return null;
					}
				}	
			});
			Pair<Account, Set<ExternalAccountView>> accountsPair = TxUtils.runInTransaction(new Callable<Pair<Account, Set<ExternalAccountView>>>() {
				public Pair<Account, Set<ExternalAccountView>> call() {
					    Account account = accounts.lookupAccountByUser(user);
					    UserViewpoint viewpoint = new UserViewpoint(account.getOwner(), Site.MUGSHOT);
						Set<ExternalAccountView> allAccounts = externalAccounts.getExternalAccountViews(viewpoint, account.getOwner());
				        externalAccounts.loadThumbnails(viewpoint, allAccounts);
				        return new Pair<Account, Set<ExternalAccountView>>(account, allAccounts);
				}	
			});
		    if (facebookAccount != null && facebookAccount.isApplicationEnabled()) {
		        FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
			    ws.setProfileFbml(facebookAccount, createFbmlForUser(accountsPair.getFirst(), accountsPair.getSecond()));
            }		    
		} catch (Exception e) {
			logger.error("Caught an exception when getting a FacebookAccount for {}: {}", user, e.getMessage());
			throw new RuntimeException(e);            			
		}
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
		Pair<Long, Long> times = ws.updateNotifications(facebookAccount);
		long messagesTime = times.getFirst();
		long pokesTime = times.getSecond();
		if (messagesTime != -1) {
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
		if (pokesTime != -1) {
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
		if (facebookAccount.isSessionKeyValid()) {
		    FacebookEvent newWallMessagesEvent = ws.updateWallMessageCount(facebookAccount);
		    if (newWallMessagesEvent != null) {
		    	// we create an individual event for each wall messages update
				persistFacebookEvent(newWallMessagesEvent);
		    	notifier.onFacebookEvent(user, newWallMessagesEvent);		    	
		    }
		} else {
			notifier.onFacebookEvent(user, getLoginStatusEvent(facebookAccount, false));
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void updateTaggedPhotos(final long facebookAccountId) {
		FacebookAccount facebookAccount;
		try {
			facebookAccount = TxUtils.runInTransaction(new Callable<FacebookAccount>() {
				public FacebookAccount call() {
					return em.find(FacebookAccount.class, facebookAccountId);		
				}	
			});
		} catch (Exception e) {
			logger.error("Failed to find facebookAccount for " + facebookAccountId, e.getMessage());
			throw new RuntimeException(e);
		}
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateTaggedPhotos()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		
		int newPhotoCount = ws.getTaggedPhotosCount(facebookAccount);
			
		FacebookTracker proxy = EJBUtil.defaultLookup(FacebookTracker.class);
		if (newPhotoCount == -1) {
			if (!facebookAccount.isSessionKeyValid()) {
				// delete cache because the photo data is not storable past the end of the session				
				taggedPhotosCache.deleteCache(user.getGuid().toString());
				// that was a detached copy of facebookAccount, we need to set isSessionKeyValid on an attached copy
				proxy.handleExpiredSessionKey(facebookAccountId);
		    }
			return;			
		}
	
		// we no longer need to delete cache if it is expired in here, because we always do it when the Facebook 
		// session is no longer valid, so if cache is expired here it must mean we couldn't get the new results
		// before, but there is no need to explicitly delete the old results
		// ExpiredCacheException is a subtype of NotCachedException, so we don't need to catch it explicitly
		boolean needCacheUpdate = false;       	
		int cachedPhotoCount = -1;
		try {
			List<? extends FacebookPhotoDataView> cachedPhotos = taggedPhotosCache.checkCache(user.getGuid().toString());
			cachedPhotoCount = cachedPhotos.size();
		} catch (NotCachedException e) {
			needCacheUpdate = true;
		}
	
		if (needCacheUpdate || (newPhotoCount != cachedPhotoCount)) {
			if ((cachedPhotoCount != -1) && (newPhotoCount != cachedPhotoCount)) {
				taggedPhotosCache.expireCache(user.getGuid().toString());
			}
			
			List<? extends FacebookPhotoDataView> cachedPhotos = taggedPhotosCache.getSync(user.getGuid().toString());
			
			proxy.saveUpdatedTaggedPhotos(facebookAccountId, cachedPhotos);
		}	
	}
	
	public void saveUpdatedTaggedPhotos(long facebookAccountId, List<? extends FacebookPhotoDataView> cachedPhotos) { 
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);	
		if (!facebookAccount.getTaggedPhotosPrimed() && cachedPhotos.isEmpty()) {
			// this covers the case when the user did not have any photos tagged with them on facebook prior 
			// to adding their facebook account to mugshot
			facebookAccount.setTaggedPhotosPrimed(true);
			return;
		}
			
		Set<FacebookPhotoDataStatus> oldPhotos = facebookAccount.getTaggedPhotos();
		List<? extends FacebookPhotoDataView> newPhotosToAdd = new ArrayList<FacebookPhotoDataView>(cachedPhotos);
		
		// We are not doing anything with photo data statuses we couldn't match up with new photos,
		// because deleting these photos would make us restack them if the web service glitched and
		// only returned a subset of photos at some point. Keeping them around should be harmless,
		// we simply wouldn't find cached photos for them, so will not display them.
		for (FacebookPhotoDataStatus oldPhoto : oldPhotos) {
			FacebookPhotoDataView foundPhoto = null;
			for (FacebookPhotoDataView newPhoto : newPhotosToAdd) {
				if (oldPhoto.matchPhotoId(newPhoto.getPhotoId())) {	
					// store the Facebook photo id for the photo if it is not yet stored
                    if (oldPhoto.getFacebookPhotoId() == null) {
					    oldPhoto.setRecoveredFacebookPhotoId(newPhoto.getPhotoId());			
                    } 
					foundPhoto = newPhoto;
					break;
				}
			}
			if (foundPhoto != null)
			    newPhotosToAdd.remove(foundPhoto);
		}
		
		if (newPhotosToAdd.size() > 0) {
			if (!facebookAccount.getTaggedPhotosPrimed()) {
				// this covers the case when the user has some photos tagged with them on facebook prior to adding
				// their facebook account to mugshot
			    for (FacebookPhotoDataView newPhotoToAdd : newPhotosToAdd) {
			    	FacebookPhotoDataStatus photoDataStatus = new FacebookPhotoDataStatus(facebookAccount, newPhotoToAdd.getPhotoId());
				    em.persist(photoDataStatus);
				    facebookAccount.addTaggedPhoto(photoDataStatus);
			    }
				facebookAccount.setTaggedPhotosPrimed(true);
				return;
			}
				
		    FacebookEvent taggedPhotosEvent = new FacebookEvent(facebookAccount, FacebookEventType.NEW_TAGGED_PHOTOS_EVENT, 
                                                                newPhotosToAdd.size(), (new Date()).getTime());		
			persistFacebookEvent(taggedPhotosEvent);
		    for (FacebookPhotoDataView newPhotoToAdd : newPhotosToAdd) {
		    	FacebookPhotoDataStatus photoDataStatus = new FacebookPhotoDataStatus(facebookAccount, newPhotoToAdd.getPhotoId());
		    	photoDataStatus.setFacebookEvent(taggedPhotosEvent);
			    em.persist(photoDataStatus);
			    taggedPhotosEvent.addPhoto(photoDataStatus);
			    facebookAccount.addTaggedPhoto(photoDataStatus);
		    }
		 
		    notifier.onFacebookEvent(facebookAccount.getExternalAccount().getAccount().getOwner(), taggedPhotosEvent);
		}
	}

	// Not currently used, we now delete cached photos as soon as the session key expires.
	// Now that Facebook has infinite session keys, we don't need to squeeze out the additional 12 hours or so
	// of keeping the cached photos, particularly becuase their terms of service seem to have changed to
	// say that we must remove stuff no later than the session expires "or such other time as Facebook may 
	// specify to you from time to time". Go figure what that means :).
	// This also allows us to not expire photos as often, cutting back on the number of requests we make to Facebook.
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
		handleExpiredSessionKey(attachedFacebookAccount);	
	}
	
	public void handleExpiredSessionKey(FacebookAccount facebookAccount) {
		facebookAccount.setSessionKeyValid(false);
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		notifier.onFacebookEvent(user, getLoginStatusEvent(facebookAccount, false));	
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
	
	private String createFbmlForUser(Account account, Set<ExternalAccountView> allAccounts) {
		User user = account.getOwner();

		StringBuilder fbmlSb = new StringBuilder("");
		fbmlSb.append("<fb:fbml version='1.1'><fb:visible-to-owner><fb:subtitle>" +
		              "<a href='http://apps.facebook.com/mugshot'>Edit Accounts</a>" +
		              "</fb:subtitle></fb:visible-to-owner></fb:fbml>");
		
		// add the accounts ribbon
        List<ExternalAccountView> lovedAccounts = new ArrayList<ExternalAccountView>();
		for (ExternalAccountView a : allAccounts) {
			// This will include the Website account which there is no way to specify
			// on the Facebook application page, but that's ok.
			// Let's exclude Facebook account itself, since it would take the user to the page
			// they are already viewing!
			if (a.getExternalAccount().isLovedAndEnabled() && !a.getExternalAccountType().equals(ExternalAccountType.FACEBOOK))
				lovedAccounts.add(a);
		}
	
		Collections.sort(lovedAccounts, new Comparator<ExternalAccountView>() {
			public int compare(ExternalAccountView first, ExternalAccountView second) {
				return ExternalAccount.compare(first.getExternalAccount(), second.getExternalAccount());	
			}			
		});
		
		fbmlSb.append("<div style='background: url(\"http://dogfood.mugshot.org/images3/facebook_gradient_bottom.gif\") bottom left repeat-x;'>");
		
		fbmlSb.append("<div style='float:left;'>Find me online:</div>");
		
		fbmlSb.append("<div style='float:left;clear:right;'>");
		for (ExternalAccountView a : lovedAccounts) {
            String imageTitle = a.getExternalAccount().getSiteName();
            if (a.getExternalAccount().getLinkText().length() >0 )
            	imageTitle = imageTitle + ": " + a.getExternalAccount().getLinkText();
          			
			fbmlSb.append("<a target='_blank' href='" + a.getLink() + "'>" +
					      "<img src='http://mugshot.org/images3/" + a.getIconName() + "' title='" + imageTitle + "' style='width: 16; height: 16; border: none; margin-right: 3px;'/>" +
					      "</a>");
		}		

		fbmlSb.append("</div></div>");
		
		Pageable<BlockView> pageableMugshot = new Pageable<BlockView>("mugshot");
		pageableMugshot.setPosition(0);
		pageableMugshot.setInitialPerPage(INITIAL_BLOCKS_PER_PAGE);
		pageableMugshot.setFlexibleResultCount(true);
		stacker.pageStack(AnonymousViewpoint.getInstance(Site.NONE), user, pageableMugshot, true);
		int resultsCount = 0;
		String backgroundColor = "#FFFFFF";
		for (BlockView blockView : pageableMugshot.getResults()) {
			if (resultsCount == INITIAL_BLOCKS_PER_PAGE - 1) {
				resultsCount++;
				break;
			}
			backgroundColor = (resultsCount % 2 == 0 ? "#FFFFFF" : "#EEEEEE");
			resultsCount++;
			fbmlSb.append(
			    "<table cellspacing='0' cellpadding='0' style='background-color: " + backgroundColor + ";width:100%;'>" +
			    "<tbody><tr><td>" +
	            "<img src='http://mugshot.org" + blockView.getIcon() + "' title='" + blockView.getTypeTitle() + "' style='width: 16; height: 16; border: none; margin-right: 3px;'/>" +
			    "</td><td>" +
			    blockView.getSummaryHeading() +
		        ": <a target='_blank' href='" + getAbsoluteUrl(blockView.getSummaryLink()) + "'>" + blockView.getSummaryLinkText() + "</a>" +
			    "</td></tr></table>");			
		}
		// display a note if there was no activity
		if (resultsCount == 0) {
			fbmlSb.append("<div>Once there are new updates, they will show up here.</div>");
		}
		if (account.getHasAcceptedTerms()) {
			String visitMugshotText = "Visit my Mugshot Page"; 
			if (resultsCount == INITIAL_BLOCKS_PER_PAGE);
			    visitMugshotText = visitMugshotText + " To See More";
			fbmlSb.append("<div style='background: url(\"http://dogfood.mugshot.org/images3/facebook_gradient_top.gif\") top left repeat-x;text-align: center;'>");    
		    fbmlSb.append("<a target='_blank' style='font-size:12px;' href='" + getAbsoluteUrl("/person?who=" + user.getId().toString()) + "'>" +
				          visitMugshotText + "</a>");
		    fbmlSb.append("</div>");
		}
		return fbmlSb.toString();
	}
	
	private String getAbsoluteUrl(String link)  {
		if (link.startsWith("/")) {
			String baseurl = config.getBaseUrlMugshot().toExternalForm();
			return baseurl + link;
		}		
		return link;
	}
}
