package com.dumbhippo.services.caches;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedFlickrUserPhotoset;
import com.dumbhippo.services.caches.FlickrUserPhotosetsCache;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.FlickrPhotosets;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class FlickrUserPhotosetsCacheBean extends AbstractListCacheBean<String,FlickrPhotosetView,CachedFlickrUserPhotoset> implements
		FlickrUserPhotosetsCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrUserPhotosetsCacheBean.class);

	// this is long, because we explicitly drop the cache if we think there 
	// might be a change... having an expiration here at all isn't needed in 
	// theory.
	static private final long FLICKR_USER_PHOTOSETS_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
	
	public FlickrUserPhotosetsCacheBean() {
		super(Request.FLICKR_USER_PHOTOSETS, FlickrUserPhotosetsCache.class, FLICKR_USER_PHOTOSETS_EXPIRATION);
	}

	@Override
	protected List<CachedFlickrUserPhotoset> queryExisting(String key) {
		Query q = em.createQuery("SELECT photo FROM CachedFlickrUserPhotoset photo WHERE photo.ownerId = :ownerId");
		q.setParameter("ownerId", key);
		
		List<CachedFlickrUserPhotoset> results = TypeUtils.castList(CachedFlickrUserPhotoset.class, q.getResultList());
		return results;
	}

	@Override
	protected void setAllLastUpdatedToZero(String key) {
		Query q = em.createQuery("UPDATE CachedFlickrUserPhotoset photoset " + 
				" SET photoset.lastUpdated = 0 " + 
				" WHERE photoset.ownerId = :ownerId");
		q.setParameter("ownerId", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired", updated);
	}	
	
	@Override
	protected FlickrPhotosetView resultFromEntity(CachedFlickrUserPhotoset entity) {
		return entity.toPhotoset();
	}

	@Override
	protected CachedFlickrUserPhotoset entityFromResult(String key, FlickrPhotosetView result) {
		return new CachedFlickrUserPhotoset(key, result);
	}

	@Override
	protected List<FlickrPhotosetView> fetchFromNetImpl(String key) {
		FlickrWebServices ws = new FlickrWebServices(REQUEST_TIMEOUT, config);
		FlickrPhotosets photosets = ws.lookupPublicPhotosets(key);
		return TypeUtils.castList(FlickrPhotosetView.class, photosets.getSets());
	}

	@Override
	protected CachedFlickrUserPhotoset newNoResultsMarker(String key) {
		return CachedFlickrUserPhotoset.newNoResultsMarker(key);
	}
}
