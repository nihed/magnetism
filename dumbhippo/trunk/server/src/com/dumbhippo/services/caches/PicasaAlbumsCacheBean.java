package com.dumbhippo.services.caches;

import java.util.List;

import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.caches.CachedPicasaAlbum;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.PicasaAlbum;
import com.dumbhippo.services.PicasaAlbumFeedParser;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class PicasaAlbumsCacheBean extends AbstractListCacheWithStorageBean<String,PicasaAlbum,CachedPicasaAlbum> implements
		PicasaAlbumsCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PicasaAlbumsCacheBean.class);

	static private final long ALBUM_EXPIRATION = 1000 * 60 * 60 * 24 * 7;
	
	static private final int NUMBER_OF_SAMPLE_ALBUMS = 7; 	
	
	public PicasaAlbumsCacheBean() {
		super(Request.PICASA_ALBUMS, PicasaAlbumsCache.class, ALBUM_EXPIRATION, PicasaAlbum.class);
	}

	@Override
	public List<CachedPicasaAlbum> queryExisting(String key) {
		Query q = em.createQuery("SELECT video FROM CachedPicasaAlbum video WHERE video.owner = :owner");
		q.setParameter("owner", key);
		
		List<CachedPicasaAlbum> results = TypeUtils.castList(CachedPicasaAlbum.class, q.getResultList());
		return results;
	}

	@Override
	public void setAllLastUpdatedToZero(String key) {
		EJBUtil.prepareUpdate(em, CachedPicasaAlbum.class);
		
		Query q = em.createQuery("UPDATE CachedPicasaAlbum" + 
				" SET lastUpdated = '1970-01-01 00:00:00' " + 
				" WHERE owner = :owner");
		q.setParameter("owner", key);
		int updated = q.executeUpdate();
		logger.debug("{} cached items expired", updated);
	}	
	
	@Override
	public PicasaAlbum resultFromEntity(CachedPicasaAlbum entity) {
		return entity.toThumbnail();
	}

	@Override
	public CachedPicasaAlbum entityFromResult(String key, PicasaAlbum result) {
		return new CachedPicasaAlbum(key, result);
	}

	@Override
	protected List<PicasaAlbum> fetchFromNetImpl(String key) {
		return PicasaAlbumFeedParser.getAlbumsForUser(key, NUMBER_OF_SAMPLE_ALBUMS);
	}

	@Override
	public CachedPicasaAlbum newNoResultsMarker(String key) {
		return CachedPicasaAlbum.newNoResultsMarker(key);
	}
}
