package com.dumbhippo.services.caches;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookPhotoDataStatus;
import com.dumbhippo.persistence.caches.CachedFacebookPhotoData;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.FacebookPhotoData;
import com.dumbhippo.services.FacebookPhotoDataView;
import com.dumbhippo.services.FacebookWebServices;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class FacebookPhotoDataCacheBean 
    extends AbstractListCacheWithStorageBean<String,FacebookPhotoDataView,CachedFacebookPhotoData> 
    implements FacebookPhotoDataCache {

	static private final Logger logger = GlobalSetup.getLogger(FacebookPhotoDataCacheBean.class);

	@EJB
	protected FacebookSystem facebookSystem;	
	
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
		super(Request.FACEBOOK_PHOTO_DATA, FacebookPhotoDataCache.class, FACEBOOK_PHOTO_DATA_EXPIRATION, FacebookPhotoDataView.class);
	}

	@Override
	public List<CachedFacebookPhotoData> queryExisting(String key) {
		Query q = em.createQuery("SELECT photo FROM CachedFacebookPhotoData photo WHERE photo.userId = :userId");
		q.setParameter("userId", key);
		List<CachedFacebookPhotoData> results = TypeUtils.castList(CachedFacebookPhotoData.class, q.getResultList());
		return results;
	}
	
	public List<FacebookPhotoDataView> queryExisting(String key, Set<FacebookPhotoDataStatus> photos) {
		List<FacebookPhotoDataView> results = new ArrayList<FacebookPhotoDataView>();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT photo FROM CachedFacebookPhotoData photo WHERE photo.userId = :userId" +
				      " AND photo.facebookPhotoId IN (");
		boolean facebookPhotoIdsAvailable = false; 
		for (FacebookPhotoDataStatus photoStatus : photos) {
			if (photoStatus.getFacebookPhotoId() != null) {
			    sb.append(photoStatus.getFacebookPhotoId());
			    sb.append(",");
			    facebookPhotoIdsAvailable = true; 
			}
		}
		
		// no need to do the query if we don't have a single id
		if (!facebookPhotoIdsAvailable) {
			return results;
		}
		
		sb.setLength(sb.length() - 1); // chop comma
		sb.append(")");
        Query q = em.createQuery(sb.toString());	
		q.setParameter("userId", key);
		List<CachedFacebookPhotoData> entities = TypeUtils.castList(CachedFacebookPhotoData.class, q.getResultList());
		for (CachedFacebookPhotoData entity : entities) {
			results.add(resultFromEntity(entity));
		}
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
	
	@Override
	public FacebookPhotoDataView resultFromEntity(CachedFacebookPhotoData entity) {
		return entity.toPhotoData();
	}

	@Override
	public CachedFacebookPhotoData entityFromResult(String key, FacebookPhotoDataView result) {
		return new CachedFacebookPhotoData(key, result);
	}

	@Override
	protected List<FacebookPhotoDataView> fetchFromNetImpl(String key) {
		FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
	    FacebookAccount facebookAccount = lookupFacebookAccount(key);
	    // this facebookAccount is detached
		List<FacebookPhotoData> photos = ws.getTaggedPhotos(facebookAccount);
		
        // we don't want to call facebookTracker.handleExpiredSessionKey(facebookAccount.getId());
		// here because we'll handle that when we are updating tagged photos and we also
		// need to drop the photos cache for the user in that case, which we do there
		if (photos == null)
			return null;
		else
			return TypeUtils.castList(FacebookPhotoDataView.class, photos);
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
