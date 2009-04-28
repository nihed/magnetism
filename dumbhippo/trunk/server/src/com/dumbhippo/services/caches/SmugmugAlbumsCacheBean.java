package com.dumbhippo.services.caches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.caches.CachedSmugmugAlbum;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.smugmug.rest.bind.Image;
import com.dumbhippo.services.smugmug.SmugMugWebServices;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class SmugmugAlbumsCacheBean extends
		AbstractListCacheWithStorageBean<String, Image, CachedSmugmugAlbum>
		implements SmugmugAlbumsCache {

	static private final Logger logger = GlobalSetup
			.getLogger(SmugmugAlbumsCacheBean.class);

	static private final long ALBUM_EXPIRATION = 1000;

	private static int IMAGE_COUNT = 5;

	public SmugmugAlbumsCacheBean() {
		super(Request.SMUGMUG_ALBUM, SmugmugAlbumsCache.class,
				ALBUM_EXPIRATION, Image.class);
	}

	@Override
	public List<CachedSmugmugAlbum> queryExisting(String key) {
		logger.debug("Cached smugmug album owner - " + key);
		Query q = em
				.createQuery("SELECT video FROM CachedSmugmugAlbum video where video.owner = :owner");
		q.setParameter("owner", key);
		List<CachedSmugmugAlbum> results = TypeUtils.castList(
				CachedSmugmugAlbum.class, q.getResultList());
		java.util.Collections.reverse(results);
		if (results.size() > IMAGE_COUNT)
		  results = results.subList(0, IMAGE_COUNT);
		return results;
	}

	@Override
	public void setAllLastUpdatedToZero(String key) {
		EJBUtil.prepareUpdate(em, CachedSmugmugAlbum.class);

		Query q = em.createQuery("UPDATE CachedSmugmugAlbum"
				+ " SET lastUpdated = '1970-01-01 00:00:00' "
				+ "where owner = :owner");
		q.setParameter("owner", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired", updated);
	}

	@Override
	public Image resultFromEntity(CachedSmugmugAlbum entity) {
		return entity.toImage();
	}

	@Override
	public CachedSmugmugAlbum entityFromResult(String key, Image result) {
		return new CachedSmugmugAlbum(key, result);
	}

	@Override
	protected List<Image> fetchFromNetImpl(String nickName) {
		List<Image> list = null;
		try {
			String apiKey = config
					.getPropertyNoDefault(com.dumbhippo.server.HippoProperty.SMUGMUG_API_KEY);
			//String apiKey = "Ett34Z1b2TA16pkotZnXBrE1dwBZsAw7";
			SmugMugWebServices ws = new SmugMugWebServices(1000 * 30, apiKey);
			Image[] images;
			list = new ArrayList<Image>();
			images = ws.getLastImages(nickName, IMAGE_COUNT);
			logger.debug("fetchFromNetImpl apiKey, nickName " + apiKey + ", " + nickName);
			list = Arrays.asList(images);
		} catch (com.dumbhippo.server.Configuration.PropertyNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public CachedSmugmugAlbum newNoResultsMarker(String key) {
		return CachedSmugmugAlbum.newNoResultsMarker(key);
	}
}
