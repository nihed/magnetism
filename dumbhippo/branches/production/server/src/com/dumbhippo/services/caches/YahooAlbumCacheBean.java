package com.dumbhippo.services.caches;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.CachedYahooAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class YahooAlbumCacheBean
	extends AbstractBasicCacheWithStorageBean<String,YahooAlbumData,CachedYahooAlbumData>
	implements YahooAlbumCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooAlbumCacheBean.class);

	public YahooAlbumCacheBean() {
		super(Request.YAHOO_ALBUM, YahooAlbumCache.class, YAHOO_EXPIRATION_TIMEOUT);
	}

	@Override
	public CachedYahooAlbumData queryExisting(String albumId) {
		Query q;
		
		q = em.createQuery("FROM CachedYahooAlbumData album WHERE album.albumId = :albumId");
		q.setParameter("albumId", albumId);
		
		try {
			return (CachedYahooAlbumData) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@Override
	protected YahooAlbumData fetchFromNetImpl(String albumId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		YahooAlbumData data = ws.lookupAlbum(albumId);
		logger.debug("Fetched album data for id {}: {}", albumId, data);
		return data;
	}

	@Override
	public YahooAlbumData resultFromEntity(CachedYahooAlbumData entity) {
		return entity.toData();
	}

	@Override
	public CachedYahooAlbumData entityFromResult(String key, YahooAlbumData result) {
		CachedYahooAlbumData entity = new CachedYahooAlbumData();
		entity.updateData(key, result);
		return entity;
	}

	@Override
	public void updateEntityFromResult(String key, YahooAlbumData result, CachedYahooAlbumData entity) {
		entity.updateData(key, result);
	}

	@Override
	public CachedYahooAlbumData newNoResultsMarker(String key) {
		return CachedYahooAlbumData.newNoResultsMarker(key);
	}
}
