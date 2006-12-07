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
import com.dumbhippo.persistence.CachedFlickrPhotosets;
import com.dumbhippo.persistence.CachedFlickrUserPhotoset;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.FlickrPhotosetView;
import com.dumbhippo.services.FlickrPhotosets;
import com.dumbhippo.services.FlickrPhotosetsView;
import com.dumbhippo.services.FlickrWebServices;

@TransactionAttribute(TransactionAttributeType.REQUIRED) // because the base classes change the default; not sure this is needed, but can't hurt
@Stateless
public class FlickrUserPhotosetsCacheBean extends AbstractBasicCacheBean<String,FlickrPhotosetsView> implements
		FlickrUserPhotosetsCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FlickrUserPhotosetsCacheBean.class);

	// this is long, because we explicitly drop the cache if we think there 
	// might be a change... having an expiration here at all isn't needed in 
	// theory.
	static private final long FLICKR_USER_PHOTOSETS_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
	
	private BasicCacheStorage<String,FlickrPhotosetsView,CachedFlickrPhotosets> summaryStorage;
	private ListCacheStorage<String,FlickrPhotosetView,CachedFlickrUserPhotoset> setListStorage;
	
	public FlickrUserPhotosetsCacheBean() {
		super(Request.FLICKR_USER_PHOTOSETS, FlickrUserPhotosetsCache.class, FLICKR_USER_PHOTOSETS_EXPIRATION);
	}

	@PostConstruct
	public void init() {
		BasicCacheStorageMapper<String,FlickrPhotosetsView,CachedFlickrPhotosets> summaryMapper =
			new BasicCacheStorageMapper<String,FlickrPhotosetsView,CachedFlickrPhotosets>() {

				public CachedFlickrPhotosets newNoResultsMarker(String key) {
					EJBUtil.assertHaveTransaction();
					
					return CachedFlickrPhotosets.newNoResultsMarker(key);
				}

				public CachedFlickrPhotosets queryExisting(String key) {
					EJBUtil.assertHaveTransaction();
					
					Query q = em.createQuery("SELECT photosets FROM CachedFlickrPhotosets photosets WHERE photosets.flickrId = :flickrId");
					q.setParameter("flickrId", key);
					
					try {
						return (CachedFlickrPhotosets) q.getSingleResult();
					} catch (NoResultException e) {
						return null;
					}
				}

				public FlickrPhotosetsView resultFromEntity(CachedFlickrPhotosets entity) {
					return entity.toPhotosets();
				}

				public CachedFlickrPhotosets entityFromResult(String key, FlickrPhotosetsView result) {
					return new CachedFlickrPhotosets(key, result);
				}

				public void updateEntityFromResult(String key, FlickrPhotosetsView result, CachedFlickrPhotosets entity) {
					entity.update(result);
				}
			
		};
		
		ListCacheStorageMapper<String,FlickrPhotosetView,CachedFlickrUserPhotoset> photoListMapper =
			new ListCacheStorageMapper<String,FlickrPhotosetView,CachedFlickrUserPhotoset>() {
			
			public List<CachedFlickrUserPhotoset> queryExisting(String key) {
				Query q = em.createQuery("SELECT photo FROM CachedFlickrUserPhotoset photo WHERE photo.ownerId = :ownerId");
				q.setParameter("ownerId", key);
				
				List<CachedFlickrUserPhotoset> results = TypeUtils.castList(CachedFlickrUserPhotoset.class, q.getResultList());
				return results;
			}

			public void setAllLastUpdatedToZero(String key) {
				EJBUtil.prepareUpdate(em, CachedFlickrUserPhotoset.class);
				
				Query q = em.createQuery("UPDATE CachedFlickrUserPhotoset c" + 
						" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
						" WHERE c.ownerId = :ownerId");
				q.setParameter("ownerId", key);
				int updated = q.executeUpdate();
				logger.debug("{} cached items expired", updated);
			}				

			public FlickrPhotosetView resultFromEntity(CachedFlickrUserPhotoset entity) {
				return entity.toPhotoset();
			}

			public CachedFlickrUserPhotoset entityFromResult(String key, FlickrPhotosetView result) {
				return new CachedFlickrUserPhotoset(key, result);
			}
			
			public CachedFlickrUserPhotoset newNoResultsMarker(String key) {
				return CachedFlickrUserPhotoset.newNoResultsMarker(key);
			}
		};
		
		summaryStorage = new BasicCacheStorage<String,FlickrPhotosetsView,CachedFlickrPhotosets>(em, getExpirationTime(), summaryMapper);
		setListStorage = new ListCacheStorage<String,FlickrPhotosetView,CachedFlickrUserPhotoset>(em, getExpirationTime(), FlickrPhotosetView.class, photoListMapper);
	}	
	

	@Override
	protected FlickrPhotosetsView fetchFromNetImpl(String key) {
		FlickrWebServices ws = new FlickrWebServices(REQUEST_TIMEOUT, config);
		FlickrPhotosets photosets = ws.lookupPublicPhotosets(key);
		return photosets;
	}
	
	public FlickrPhotosetsView checkCache(String key) throws NotCachedException {
		FlickrPhotosetsView summary = summaryStorage.checkCache(key);
		List<? extends FlickrPhotosetView> setList = setListStorage.checkCache(key);
		summary.setSets(setList);
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public FlickrPhotosetsView saveInCacheInsideExistingTransaction(String key, FlickrPhotosetsView data, Date now, boolean refetchedWithoutCheckingCache) {
		if (refetchedWithoutCheckingCache) {
			try {
				// Try to avoid saving to the cache if the photoset count hasn't changed. (Yes, this could lead to not 
				// getting new info if someone adds and deletes photosets for a net change of 0; but after the expiration of 
				// the cache, checkCache here will return NotCachedException and we'll recover. Or as soon as someone 
				// adds or removes another photoset we'll recover.)
				FlickrPhotosetsView old = checkCache(key);
				if (old.getTotal() == data.getTotal())
					return old;
			} catch (NotCachedException e) {
				// we need to use the new results
			}
		}
		
		FlickrPhotosetsView summary = summaryStorage.saveInCacheInsideExistingTransaction(key, data, now, refetchedWithoutCheckingCache);
		List<? extends FlickrPhotosetView> setList = setListStorage.saveInCacheInsideExistingTransaction(key, data.getSets(), now, refetchedWithoutCheckingCache);
		summary.setSets(setList);
		return summary;
	}

	@Override
	public void expireCache(String key) {
		EJBUtil.assertHaveTransaction();
		
		summaryStorage.expireCache(key);
		setListStorage.expireCache(key);
	}
}
