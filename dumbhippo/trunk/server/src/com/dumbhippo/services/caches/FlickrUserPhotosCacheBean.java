package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedFlickrPhotos;
import com.dumbhippo.persistence.CachedFlickrUserPhoto;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.FlickrPhotoView;
import com.dumbhippo.services.FlickrPhotos;
import com.dumbhippo.services.FlickrPhotosView;
import com.dumbhippo.services.FlickrWebServices;

@Stateless
public class FlickrUserPhotosCacheBean extends AbstractBasicCacheBean<String,FlickrPhotosView> implements
		FlickrUserPhotosCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrUserPhotosCacheBean.class);

	// this is long, because we explicitly drop the cache if we think there 
	// might be a change... having an expiration here at all isn't needed in 
	// theory.
	static private final long FLICKR_USER_PHOTOS_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
	
	// how many photos we want to get for a user; we don't need to store them all,
	// just a few to display. Right now we show maybe 4 or 5, so ask for a couple 
	// extra.
	static private final int NUMBER_OF_SAMPLE_PHOTOS = 7; 

	private BasicCacheStorage<String,FlickrPhotosView,CachedFlickrPhotos> summaryStorage;
	private ListCacheStorage<String,FlickrPhotoView,CachedFlickrUserPhoto> photoListStorage;
	
	public FlickrUserPhotosCacheBean() {
		super(Request.FLICKR_USER_PHOTOS, FlickrUserPhotosCache.class, FLICKR_USER_PHOTOS_EXPIRATION);
	}
	
	@PostConstruct
	public void init() {
		BasicCacheStorageMapper<String,FlickrPhotosView,CachedFlickrPhotos> summaryMapper =
			new BasicCacheStorageMapper<String,FlickrPhotosView,CachedFlickrPhotos>() {

				public CachedFlickrPhotos newNoResultsMarker(String key) {
					EJBUtil.assertHaveTransaction();
					
					return CachedFlickrPhotos.newNoResultsMarker(key);
				}

				public CachedFlickrPhotos queryExisting(String key) {
					EJBUtil.assertHaveTransaction();
					
					Query q = em.createQuery("SELECT photos FROM CachedFlickrPhotos photos WHERE photos.flickrId = :flickrId");
					q.setParameter("flickrId", key);
					
					try {
						return (CachedFlickrPhotos) q.getSingleResult();
					} catch (NoResultException e) {
						return null;
					}
				}

				public FlickrPhotosView resultFromEntity(CachedFlickrPhotos entity) {
					return entity.toPhotos();
				}

				public CachedFlickrPhotos entityFromResult(String key, FlickrPhotosView result) {
					return new CachedFlickrPhotos(key, result);
				}

				public void updateEntityFromResult(String key, FlickrPhotosView result, CachedFlickrPhotos entity) {
					entity.update(result);
				}
			
		};
		
		ListCacheStorageMapper<String,FlickrPhotoView,CachedFlickrUserPhoto> photoListMapper =
			new ListCacheStorageMapper<String,FlickrPhotoView,CachedFlickrUserPhoto>() {

			public List<CachedFlickrUserPhoto> queryExisting(String key) {
				EJBUtil.assertHaveTransaction();
				
				Query q = em.createQuery("SELECT photo FROM CachedFlickrUserPhoto photo WHERE photo.ownerId = :ownerId");
				q.setParameter("ownerId", key);
				
				List<CachedFlickrUserPhoto> results = TypeUtils.castList(CachedFlickrUserPhoto.class, q.getResultList());
				return results;
			}

			public void setAllLastUpdatedToZero(String key) {
				EJBUtil.assertHaveTransaction();
				
				EJBUtil.prepareUpdate(em, CachedFlickrUserPhoto.class);
				
				Query q = em.createQuery("UPDATE CachedFlickrUserPhoto c " + 
						" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
						" WHERE c.ownerId = :ownerId");
				q.setParameter("ownerId", key);
				int updated = q.executeUpdate();
				logger.debug("{} cached items expired", updated);
			}	
			
			public FlickrPhotoView resultFromEntity(CachedFlickrUserPhoto entity) {
				return entity.toPhoto();
			}

			public CachedFlickrUserPhoto entityFromResult(String key, FlickrPhotoView result) {
				return new CachedFlickrUserPhoto(key, result);
			}
			
			public CachedFlickrUserPhoto newNoResultsMarker(String key) {
				return CachedFlickrUserPhoto.newNoResultsMarker(key);
			}
		};
		
		summaryStorage = new BasicCacheStorage<String,FlickrPhotosView,CachedFlickrPhotos>(em, getExpirationTime(), summaryMapper);
		photoListStorage = new ListCacheStorage<String,FlickrPhotoView,CachedFlickrUserPhoto>(em, getExpirationTime(), FlickrPhotoView.class, photoListMapper);
	}
	

	@Override
	protected FlickrPhotosView fetchFromNetImpl(String key) {
		FlickrWebServices ws = new FlickrWebServices(REQUEST_TIMEOUT, config);
		FlickrPhotos photos = ws.lookupPublicPhotos(key, 1, NUMBER_OF_SAMPLE_PHOTOS);
		return photos;
	}

	public FlickrPhotosView checkCache(String key) throws NotCachedException {
		FlickrPhotosView summary = summaryStorage.checkCache(key);
		List<? extends FlickrPhotoView> photoList = photoListStorage.checkCache(key);
		summary.setPhotos(photoList);
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public FlickrPhotosView saveInCacheInsideExistingTransaction(String key, FlickrPhotosView data, Date now) {
		FlickrPhotosView summary = summaryStorage.saveInCacheInsideExistingTransaction(key, data, now);
		List<? extends FlickrPhotoView> photoList = photoListStorage.saveInCacheInsideExistingTransaction(key, data.getPhotos(), now);
		summary.setPhotos(photoList);
		return summary;
	}

	@Override
	public void expireCache(String key) {
		EJBUtil.assertHaveTransaction();
		
		summaryStorage.expireCache(key);
		photoListStorage.expireCache(key);
	}
}
