package com.dumbhippo.services.caches;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.CachedFacebookPhotoData;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookPhotoDataStatus;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.FacebookPhotoData;
import com.dumbhippo.services.FacebookWebServices;


// FIXME the ResultType and EntityType should not be the same. It leads to all kinds of confusion. To avoid
// cut-and-paste, you can use various tricks (have an interface for ResultType that's implemented by the 
// EntityType for example). The ResultType should be the same as whatever the raw, uncached, 
// nothing-to-do-with-the-database web service returns. It's bad if anything is using attached
// EntityType objects outside of the cache code itself.
//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class FacebookPhotoDataCacheBean 
    extends AbstractListCacheWithStorageBean<String,CachedFacebookPhotoData,CachedFacebookPhotoData> 
    implements FacebookPhotoDataCache {

	static private final Logger logger = GlobalSetup.getLogger(FacebookPhotoDataCacheBean.class);

	@EJB
	protected FacebookSystem facebookSystem;	

	@EJB
	@IgnoreDependency
	protected FacebookTracker facebookTracker;	
	
	// Facebook no longer specifies a maximum time for which we can keep the data that is "not
	// storable", so we should delete the data whenever the session in which we obtained it expires.
	// We can make the expiration period here arbitrarily long to cut back on the number of requests
	// we make to Facebook, even though having it at 11 1/2 hours as it used to be was still
	// insignificant, it resulted in additional ~3 requests per person in 24 hours vs. ~393 requests
	// for updates that we currently make per person per day. (11 1/2 hour number was designed for 24 hour
	// sessions.)
	// Let's make it 14 days like for many other types of cached data. When we believe the data has changed
	// we explicitly expire it. (This might catch some new photos if the same number of photos was added
	// and deleted between our checks, which are every 11 minutes.)
	static private final long FACEBOOK_PHOTO_DATA_EXPIRATION = 1000 * 60 * 60 * 24 * 14;
	
	public FacebookPhotoDataCacheBean() {
		super(Request.FACEBOOK_PHOTO_DATA, FacebookPhotoDataCache.class, FACEBOOK_PHOTO_DATA_EXPIRATION, CachedFacebookPhotoData.class);
	}

	@Override
	public List<CachedFacebookPhotoData> queryExisting(String key) {
		Query q = em.createQuery("SELECT photo FROM CachedFacebookPhotoData photo WHERE photo.userId = :userId");
		q.setParameter("userId", key);
		List<CachedFacebookPhotoData> results = TypeUtils.castList(CachedFacebookPhotoData.class, q.getResultList());
		return results;
	}

	@Override
	public void setAllLastUpdatedToZero(String key) {
		EJBUtil.prepareUpdate(em, CachedFacebookPhotoData.class);
		
		Query q = em.createQuery("UPDATE CachedFacebookPhotoData c" + 
				" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
				" WHERE c.userId = :userId");
		q.setParameter("userId", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired for key {}", updated, key);
	}	
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void deleteCache(String key) {
		FacebookAccount facebookAccount = lookupFacebookAccount(key);
		int deletedItemsCount = 0;
		for (FacebookPhotoDataStatus photoDataStatus : facebookAccount.getTaggedPhotos()) {
			CachedFacebookPhotoData cachedPhotoData = photoDataStatus.getPhotoData();			
			if (cachedPhotoData != null) {
			    photoDataStatus.setNewPhotoData(null);
			    em.remove(cachedPhotoData);
			    deletedItemsCount++;
			} 
		}
		logger.debug("removed {} cached items for key {}", deletedItemsCount, key);

		// we might still have a no results marker which we should also remove			
		List<CachedFacebookPhotoData> oldItems = queryExisting(key);
		for (CachedFacebookPhotoData d : oldItems) {
			if (!d.isNoResultsMarker())
				logger.warn("Removing a cached photo data which was not a no results marker and was"
						    + " not associated with any photoDataStatus for the key {}", key);
			em.remove(d);
		}		
	}
	
	@Override
	public CachedFacebookPhotoData resultFromEntity(CachedFacebookPhotoData entity) {
		return entity;
	}

	@Override
	public CachedFacebookPhotoData entityFromResult(String key, CachedFacebookPhotoData result) {
		return result;
	}

	@Override
	protected List<CachedFacebookPhotoData> fetchFromNetImpl(String key) {
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
	    FacebookAccount facebookAccount = lookupFacebookAccount(key);
	    // this facebookAccount is detached
		List<FacebookPhotoData> photos = ws.getTaggedPhotos(facebookAccount);
		
		if (photos == null) {
			if (!facebookAccount.isSessionKeyValid())
		        facebookTracker.handleExpiredSessionKey(facebookAccount.getId());
		    return null;
		}
		
		List<CachedFacebookPhotoData> taggedPhotosToBeCached = new ArrayList<CachedFacebookPhotoData>();
		for (FacebookPhotoData photoData : photos) {
			CachedFacebookPhotoData photoDataToBeCached = new CachedFacebookPhotoData(key, photoData);
			taggedPhotosToBeCached.add(photoDataToBeCached);
		}
		
		return taggedPhotosToBeCached;
	}

	@Override
	public CachedFacebookPhotoData newNoResultsMarker(String key) {
		return CachedFacebookPhotoData.newNoResultsMarker(key);
	}
	
	private FacebookAccount lookupFacebookAccount(String key) {
		try {
		    FacebookAccount facebookAccount = facebookSystem.lookupFacebookAccount(SystemViewpoint.getInstance(), key);
			return facebookAccount;
	    } catch (ParseException e) {
			throw new RuntimeException("Could not parse the key " + key + " as a user guid", e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Could not find the user or facebook account for the user with guid " + key, e);
		}		
	}
}
