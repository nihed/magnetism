package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookAlbumData;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoData;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.SingletonServiceMBean;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.services.FacebookWebServices;

@Service(objectName="dumbhippo.com:service=FacebookTracker")
public class FacebookTrackerBean implements FacebookTracker, SingletonServiceMBean {
	static private final Logger logger = GlobalSetup.getLogger(FacebookTrackerBean.class);
	
	// How old the facebook data can be before we refetch
	static final long FACEBOOK_UPDATE_TIME = 10 * 60 * 1000; // 10 minutes
	
	// Interval at which we check all threads for needing update. This is shorter 
	// than FACEBOOK_UPDATE_TIME so that we don't just miss an update and wait 
	// an entire additional cycle
	static final long UPDATE_THREAD_TIME = FACEBOOK_UPDATE_TIME / 2;
	
	// how long to wait on the Facebook API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em; 
		
	@EJB
	private Configuration config;
	
	@EJB
	private Stacker stacker;
	
	// We have one FacebookTrackerBean per server instance, but only the
	// cluster singleton actually does updates.

	public void startSingleton() {
		logger.info("Starting FacebookUpdater singleton");
		FacebookUpdater.getInstance().start();
	}

	public void stopSingleton() {
		logger.info("Stopping FacebookUpdater singleton");
		FacebookUpdater.getInstance().shutdown();
	}

	public void start() throws Exception {
	}

	public void stop() throws Exception {
	}
	
	public void updateOrCreateFacebookAccount(UserViewpoint viewpoint, String facebookAuthToken) {
		ExternalAccount externalAccount = externalAccounts.getOrCreateExternalAccount(viewpoint, ExternalAccountType.FACEBOOK);
		
		FacebookAccount facebookAccount;
		if (externalAccount.getExtra() == null) {
		    facebookAccount = new FacebookAccount(externalAccount);
		    em.persist(facebookAccount);
		    externalAccount.setExtra(Long.toString(facebookAccount.getId()));
		    externalAccount.setSentiment(Sentiment.LOVE);
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
		// we want to stack an update regardless of whether they have new messages, so that they know
		// they logged in successfully; 
		stacker.stackAccountUpdateSelf(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
                ExternalAccountType.FACEBOOK, updateTime);
	}
	
	public List<FacebookAccount> getAccountsWithValidSession() {
		List list = em.createQuery("SELECT fa FROM FacebookAccount fa WHERE fa.sessionKeyValid = true")
				.getResultList();
		return TypeUtils.castList(FacebookAccount.class, list);
	}
	
	public void updateMessageCount(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		boolean newFacebookEventSelf = false;
		boolean newFacebookEventOthers = false;
		long updateTime = (new Date()).getTime();
		// we could do these requests in parallel, but be careful about updating the same facebookAccount
		long time = ws.updateMessageCount(facebookAccount);
		if (time != -1) {
    		newFacebookEventSelf = FacebookEventType.UNREAD_MESSAGES_UPDATE.getDisplayToSelf();
    		newFacebookEventOthers = FacebookEventType.UNREAD_MESSAGES_UPDATE.getDisplayToOthers();
		    updateTime = time;
		}
		if (facebookAccount.isSessionKeyValid()) {
		    FacebookEvent newWallMessagesEvent = ws.updateWallMessageCount(facebookAccount);
		    if (newWallMessagesEvent != null) {
		    	em.persist(newWallMessagesEvent);
		    	facebookAccount.addFacebookEvent(newWallMessagesEvent);
		    	newFacebookEventSelf = newWallMessagesEvent.getEventType().getDisplayToSelf();
		    	newFacebookEventOthers = newWallMessagesEvent.getEventType().getDisplayToOthers();
			    updateTime = newWallMessagesEvent.getEventTimestampAsLong();		    	
		    }
		    
		    if (facebookAccount.isSessionKeyValid()) {
		    	long pokeTime = ws.updatePokeCount(facebookAccount);
		    	if (pokeTime != -1) {
		    		newFacebookEventSelf = FacebookEventType.UNSEEN_POKES_UPDATE.getDisplayToSelf();
		    		newFacebookEventOthers = FacebookEventType.UNSEEN_POKES_UPDATE.getDisplayToOthers();
				    updateTime = pokeTime;
		    	}
		    }
		}
		// we will stack an external account update for self if we are stacking it for others
		if ((newFacebookEventSelf && !newFacebookEventOthers) || !facebookAccount.isSessionKeyValid()) {
			stacker.stackAccountUpdateSelf(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
					                       ExternalAccountType.FACEBOOK, updateTime);			
		}
		if (newFacebookEventOthers) {
			stacker.stackAccountUpdate(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
                                       ExternalAccountType.FACEBOOK, updateTime);				
		}
	}
	
	public void updateTaggedPhotos(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		List<FacebookPhotoData> newPhotos = ws.updateTaggedPhotos(facebookAccount);
		
		if (newPhotos == null) {
			if (!facebookAccount.isSessionKeyValid()) {
			    stacker.stackAccountUpdateSelf(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
					                           ExternalAccountType.FACEBOOK, (new Date()).getTime());			
		    }
			return;
		}
		
		Set<FacebookPhotoData> oldPhotos = facebookAccount.getTaggedPhotos();
		Set<FacebookPhotoData> oldPhotosToRemove = new HashSet<FacebookPhotoData>(oldPhotos);
		List<FacebookPhotoData> newPhotosToAdd = new ArrayList<FacebookPhotoData>(newPhotos);
		
		for (FacebookPhotoData oldPhoto : oldPhotos) {
			FacebookPhotoData foundPhoto = null;
			for (FacebookPhotoData newPhoto : newPhotosToAdd) {
				if (newPhoto.getSource().equals(oldPhoto.getSource())) {
					oldPhoto.updateCachedData(newPhoto);					
					oldPhotosToRemove.remove(oldPhoto);
					foundPhoto = newPhoto;
					break;
				}
			}
			if (foundPhoto != null)
			    newPhotosToAdd.remove(foundPhoto);
		}
		
		for (FacebookPhotoData oldPhotoToRemove : oldPhotosToRemove) {
			facebookAccount.removeTaggedPhoto(oldPhotoToRemove);
			// we do not remove the photo from its FacebookEvent and from the database,
			// we leave the association in
		}
		
		if (newPhotosToAdd.size() > 0) {
			if (!facebookAccount.getTaggedPhotosPrimed()) {
				// this covers the case when a user has some photos tagged with them on facebook prior to adding
				// their facebook account to mugshot
			    for (FacebookPhotoData newPhotoToAdd : newPhotosToAdd) {
				    em.persist(newPhotoToAdd);
				    facebookAccount.addTaggedPhoto(newPhotoToAdd);
			    }
				facebookAccount.setTaggedPhotosPrimed(true);
				return;
			}
				
		    FacebookEvent taggedPhotosEvent = new FacebookEvent(facebookAccount, FacebookEventType.NEW_TAGGED_PHOTOS_EVENT, 
                                                                newPhotosToAdd.size(), (new Date()).getTime());		
		    
		    em.persist(taggedPhotosEvent);
		    for (FacebookPhotoData newPhotoToAdd : newPhotosToAdd) {
		    	newPhotoToAdd.setFacebookEvent(taggedPhotosEvent);
			    em.persist(newPhotoToAdd);
			    taggedPhotosEvent.addPhoto(newPhotoToAdd);
			    facebookAccount.addTaggedPhoto(newPhotoToAdd);
		    }
		    
		    facebookAccount.addFacebookEvent(taggedPhotosEvent);
		 
		    stackEvent(taggedPhotosEvent.getEventType(), facebookAccount, taggedPhotosEvent.getEventTimestampAsLong());
		}
	}

	public void updateAlbums(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		Set<FacebookAlbumData> modifiedAlbums = ws.getModifiedAlbums(facebookAccount);
		
		if (modifiedAlbums.isEmpty()) {
			if (!facebookAccount.isSessionKeyValid()) {
			    stacker.stackAccountUpdateSelf(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
					                           ExternalAccountType.FACEBOOK, (new Date()).getTime());			
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
		    if (facebookAccount.getAlbumsModifiedTimestampAsLong() > 0) {
		    	FacebookEvent modifiedAlbumEvent = new FacebookEvent(facebookAccount, FacebookEventType.NEW_ALBUM_EVENT,
		    			                                             1, updateTime);
		    	modifiedAlbumEvent.setAlbum(newAlbumToAdd);
		    	em.persist(modifiedAlbumEvent);
		    	newAlbumToAdd.setFacebookEvent(modifiedAlbumEvent);
		    	facebookAccount.addFacebookEvent(modifiedAlbumEvent);
		    }
			if (albumsModifiedTimestamp < newAlbumToAdd.getModifiedTimestampAsLong()) {
				albumsModifiedTimestamp = newAlbumToAdd.getModifiedTimestampAsLong();
			}		    
		}
		
		if (facebookAccount.getAlbumsModifiedTimestampAsLong() > 0) {
			// this assumes that MODIFIED_ALBUM_EVENT and NEW_ALBUM_EVENT have the same stacking rules,
			// otherwise we'd need to check what event(s) happened
			stackEvent(FacebookEventType.MODIFIED_ALBUM_EVENT, facebookAccount, updateTime);
		}
		
		facebookAccount.setAlbumsModifiedTimestampAsLong(albumsModifiedTimestamp);		
	}
		
	private void stackEvent(FacebookEventType eventType, FacebookAccount facebookAccount, long updateTime) {
        // we will stack an external account update for self if we are stacking it for others
	    if (eventType.getDisplayToSelf() && !eventType.getDisplayToOthers()) {
    	    stacker.stackAccountUpdateSelf(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
                                            ExternalAccountType.FACEBOOK, updateTime);	
	    }	    
	    if (eventType.getDisplayToOthers()) {
            stacker.stackAccountUpdate(facebookAccount.getExternalAccount().getAccount().getOwner().getGuid(), 
                                       ExternalAccountType.FACEBOOK, updateTime);	
	    }		
	}
	
	private static class FacebookUpdater extends Thread {
		private static FacebookUpdater instance;
		private int generation;
		
		static synchronized FacebookUpdater getInstance() {
			if (instance == null)
				instance = new FacebookUpdater();
			
			return instance;
		}
		
		public FacebookUpdater() {
			super("FacebookUpdater");
		}
		
		private boolean runOneGeneration() {
			int iteration = 0;
			generation += 1;
			try {
                // We start off by sleeping for our delay time to reduce the initial
				// server load on restart
				long lastUpdate = System.currentTimeMillis();
				
				while (true) {
					iteration += 1;
					try {
						long sleepTime = lastUpdate + UPDATE_THREAD_TIME - System.currentTimeMillis();
						if (sleepTime < 0)
							sleepTime = 0;
						Thread.sleep(sleepTime);
						
						// We intentionally iterate here rather than inside a session
						// bean method to get a separate transaction for each method on
						// FacebookTracker rather than holding a single transaction over the whole
						// process.
						final FacebookTracker facebookTracker = EJBUtil.defaultLookup(FacebookTracker.class);
						List<FacebookAccount> facebookAccounts = facebookTracker.getAccountsWithValidSession();
						
						logger.debug("FacebookUpdater slept " + sleepTime / 1000.0 + " seconds, and now has " 
								     + facebookAccounts.size() + " facebook accounts to request info for, iteration " 
								     + iteration);
						
						ExecutorService threadPool = ThreadUtils.newCachedThreadPool("facebook fetcher " + generation + " " + iteration);
						for (final FacebookAccount facebookAccount : facebookAccounts) {
						    threadPool.execute(new Runnable() {
								public void run() {
									final FacebookTracker facebookTracker = EJBUtil.defaultLookup(FacebookTracker.class);
									// FIXME: we might end up calling web services multiple times with an expired session
									// key, perhaps have the functions should return a boolean on whether the session key
									// is still fine or unite them in one function
									facebookTracker.updateMessageCount(facebookAccount.getId());
									facebookTracker.updateTaggedPhotos(facebookAccount.getId());
									facebookTracker.updateAlbums(facebookAccount.getId());
								}
							});
						}
						// tell thread pool to terminate once all tasks are run.
						threadPool.shutdown();
						
						// The idea is to avoid "piling up" (only have one thread pool
						// doing anything at a time). There's a timeout here 
						// though and if it expired we would indeed pile up.
						if (!threadPool.awaitTermination(60 * 30, TimeUnit.SECONDS)) {
							logger.warn("FacebookUpdater thread pool timed out; some updater thread is still live and we're continuing anyway");
						}
						
						lastUpdate = System.currentTimeMillis();
					} catch (InterruptedException e) {
						break;
					}
				}
			} catch (RuntimeException e) {
				// not sure jboss will catch and print this since it's our own thread, so doing it here
				logger.error("Unexpected exception updating facebook accounts, generation " + generation + " exiting abnormally", e);
				return true; // do another generation
			}
			logger.debug("Cleanly shutting down facebook updater thread generation {}", generation);
			return false; // shut down
		}
		
		@Override
		public void run() {
			generation = 0;

			while (runOneGeneration()) {
				// sleep protects us from 100% CPU in catastrophic failure case
				logger.warn("FacebookUpdater thread sleeping and then restarting itself");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
			}
		}
		
		public void shutdown() {
			interrupt();
			
			try {
				join();
				logger.info("Successfully stopped FacebookUpdater thread");
			} catch (InterruptedException e) {
				// Shouldn't happen, just ignore
			}
	    }
	}
}
