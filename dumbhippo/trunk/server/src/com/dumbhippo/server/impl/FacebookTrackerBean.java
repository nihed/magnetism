package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookAlbumData;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.FacebookPhotoData;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.services.FacebookWebServices;

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
		notifier.onFacebookSignedIn(facebookAccount.getExternalAccount().getAccount().getOwner(),
				facebookAccount, updateTime);
	}
	
	public List<FacebookAccount> getAccountsWithValidSession() {
		List list = em.createQuery("SELECT fa FROM FacebookAccount fa WHERE fa.sessionKeyValid = true")
				.getResultList();
		return TypeUtils.castList(FacebookAccount.class, list);
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
			notifier.onFacebookEvent(user, FacebookEventType.UNREAD_MESSAGES_UPDATE, 
					facebookAccount, time);
		}
		if (facebookAccount.isSessionKeyValid()) {
		    FacebookEvent newWallMessagesEvent = ws.updateWallMessageCount(facebookAccount);
		    if (newWallMessagesEvent != null) {
		    	em.persist(newWallMessagesEvent);
		    	facebookAccount.addFacebookEvent(newWallMessagesEvent);
		    	notifier.onFacebookEvent(user, newWallMessagesEvent.getEventType(),
		    			facebookAccount, newWallMessagesEvent.getEventTimestampAsLong());		    	
		    }
		    
		    if (facebookAccount.isSessionKeyValid()) {
		    	long pokeTime = ws.updatePokeCount(facebookAccount);
		    	if (pokeTime != -1) {
		    		notifier.onFacebookEvent(user, FacebookEventType.UNSEEN_POKES_UPDATE,
		    				facebookAccount, pokeTime);
		    	}
		    }
		} else {
			notifier.onFacebookSignedOut(user, facebookAccount);
		}
	}
	
	// FIXME this is calling web services with a transaction open, which holds 
	// a db connection open so other threads can't use it, and could also 
	// time out the transaction
	public void updateTaggedPhotos(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		List<FacebookPhotoData> newPhotos = ws.updateTaggedPhotos(facebookAccount);
		
		if (newPhotos == null) {
			if (!facebookAccount.isSessionKeyValid()) {
				notifier.onFacebookSignedOut(user, facebookAccount);
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
		 
		    notifier.onFacebookEvent(user, taggedPhotosEvent.getEventType(), facebookAccount, taggedPhotosEvent.getEventTimestampAsLong());
		}
	}

	// FIXME this is calling web services with a transaction open, which holds 
	// a db connection open so other threads can't use it, and could also 
	// time out the transaction
	public void updateAlbums(long facebookAccountId) {
		FacebookAccount facebookAccount = em.find(FacebookAccount.class, facebookAccountId);
		if (facebookAccount == null)
			throw new RuntimeException("Invalid FacebookAccount id " + facebookAccountId + " is passed in to updateMessageCount()");
		User user = facebookAccount.getExternalAccount().getAccount().getOwner();
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
		Set<FacebookAlbumData> modifiedAlbums = ws.getModifiedAlbums(facebookAccount);
		
		if (modifiedAlbums.isEmpty()) {
			if (!facebookAccount.isSessionKeyValid()) {
				notifier.onFacebookSignedOut(user, facebookAccount);
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
			// otherwise we'd need to check what event(s) happened. Also this assumes that 
			// the only listener to this event notification is the stacker block handler and 
			// so we only need to send one event instead of each event.
			notifier.onFacebookEvent(user, FacebookEventType.MODIFIED_ALBUM_EVENT, facebookAccount, updateTime);
		}
		
		facebookAccount.setAlbumsModifiedTimestampAsLong(albumsModifiedTimestamp);		
	}
}
