package com.dumbhippo.server.impl;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedFlickrUserPhoto;
import com.dumbhippo.server.FlickrUserPhotosCache;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class FlickrUserPhotosCacheBean extends AbstractListCacheBean<String,FlickrPhotoView,CachedFlickrUserPhoto> implements
		FlickrUserPhotosCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrUserPhotosCacheBean.class);

	static private final long FLICKR_USER_PHOTOS_EXPIRATION = 1000 * 60 * 10; // 10 minutes
	
	public FlickrUserPhotosCacheBean() {
		super(Request.FLICKR_USER_PHOTOS, FlickrUserPhotosCache.class, FLICKR_USER_PHOTOS_EXPIRATION);
	}

	@Override
	protected List<CachedFlickrUserPhoto> queryExisting(String key) {
		Query q = em.createQuery("SELECT photo FROM CachedFlickrUserPhoto photo WHERE photo.ownerId = :ownerId");
		q.setParameter("ownerId", key);
		
		List<CachedFlickrUserPhoto> results = TypeUtils.castList(CachedFlickrUserPhoto.class, q.getResultList());
		return results;
	}

	@Override
	protected FlickrPhotoView resultFromEntity(CachedFlickrUserPhoto entity) {
		return entity.toPhoto();
	}

	@Override
	protected CachedFlickrUserPhoto entityFromResult(String key, FlickrPhotoView result) {
		return new CachedFlickrUserPhoto(key, result);
	}

	@Override
	protected List<FlickrPhotoView> fetchFromNetImpl(String key) {
		FlickrWebServices ws = new FlickrWebServices(REQUEST_TIMEOUT, config);
		FlickrPhotos photos = ws.lookupPublicPhotos(key, 1);
		return TypeUtils.castList(FlickrPhotoView.class, photos.getPhotos());
	}

	@Override
	protected CachedFlickrUserPhoto newNoResultsMarker(String key) {
		return CachedFlickrUserPhoto.newNoResultsMarker(key);
	}
}
