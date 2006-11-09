package com.dumbhippo.server.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedFlickrPhotosetPhoto;
import com.dumbhippo.server.FlickrPhotosetPhotosCache;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotoset;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class FlickrPhotosetPhotosCacheBean extends AbstractListCacheBean<String,FlickrPhotoView,CachedFlickrPhotosetPhoto> implements
		FlickrPhotosetPhotosCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrPhotosetPhotosCacheBean.class);

	// this is long, because we explicitly drop the cache if we think there 
	// might be a change... having an expiration here at all isn't needed in 
	// theory.
	static private final long FLICKR_PHOTOSET_PHOTOS_EXPIRATION = 1000 * 60 * 60 * 24 * 7; 
	
	// how many photos we want to get for a set; we don't need to store them all,
	// just a few to display. Right now we show maybe 4 or 5, so ask for a couple 
	// extra.
	static private final int NUMBER_OF_SAMPLE_PHOTOS = 7;
	
	public FlickrPhotosetPhotosCacheBean() {
		super(Request.FLICKR_PHOTOSET_PHOTOS, FlickrPhotosetPhotosCache.class, FLICKR_PHOTOSET_PHOTOS_EXPIRATION);
	}

	@Override
	protected List<CachedFlickrPhotosetPhoto> queryExisting(String key) {
		Query q = em.createQuery("SELECT photo FROM CachedFlickrPhotosetPhoto photo WHERE photo.setId = :setId");
		q.setParameter("setId", key);
		
		List<CachedFlickrPhotosetPhoto> results = TypeUtils.castList(CachedFlickrPhotosetPhoto.class, q.getResultList());
		return results;
	}

	@Override
	protected void setAllLastUpdatedToZero(String key) {
		Query q = em.createQuery("UPDATE CachedFlickrPhotosetPhoto photo " + 
				" SET photo.lastUpdated = 0 " + 
				" WHERE photo.setId = :setId");
		q.setParameter("setId", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired", updated);
	}
	
	@Override
	protected FlickrPhotoView resultFromEntity(CachedFlickrPhotosetPhoto entity) {
		return entity.toPhoto();
	}

	@Override
	protected CachedFlickrPhotosetPhoto entityFromResult(String key, FlickrPhotoView result) {
		return new CachedFlickrPhotosetPhoto(key, result);
	}

	@Override
	protected List<FlickrPhotoView> fetchFromNetImpl(String key) {
		FlickrWebServices ws = new FlickrWebServices(REQUEST_TIMEOUT, config);
		FlickrPhotoset photoset = ws.lookupPublicPhotoset(key, 1, NUMBER_OF_SAMPLE_PHOTOS);
		return TypeUtils.castList(FlickrPhotoView.class, photoset.getPhotos().getPhotos());
	}

	@Override
	protected CachedFlickrPhotosetPhoto newNoResultsMarker(String key) {
		return CachedFlickrPhotosetPhoto.newNoResultsMarker(key);
	}
}
