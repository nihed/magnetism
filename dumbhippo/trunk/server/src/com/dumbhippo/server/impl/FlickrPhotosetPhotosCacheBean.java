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

	static private final long FLICKR_PHOTOSET_PHOTOS_EXPIRATION = 1000 * 60 * 10; // 10 minutes
	
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
		FlickrPhotoset photoset = ws.lookupPublicPhotoset(key, 1);
		return TypeUtils.castList(FlickrPhotoView.class, photoset.getPhotos().getPhotos());
	}

	@Override
	protected CachedFlickrPhotosetPhoto newNoResultsMarker(String key) {
		return CachedFlickrPhotosetPhoto.newNoResultsMarker(key);
	}
}
