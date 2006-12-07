package com.dumbhippo.services.caches;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.CachedAmazonAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.services.AmazonAlbumData;
import com.dumbhippo.services.AmazonItemSearch;

@TransactionAttribute(TransactionAttributeType.REQUIRED) // because the base classes change the default; not sure this is needed, but can't hurt
@BanFromWebTier
@Stateless
public class AmazonAlbumCacheBean extends AbstractBasicCacheWithStorageBean<AlbumAndArtist,AmazonAlbumData,CachedAmazonAlbumData> implements AmazonAlbumCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonAlbumCacheBean.class);
	
	// 14 days since we aren't getting price information
	static private final int AMAZON_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 14;	
	
	public AmazonAlbumCacheBean() {
		super(Request.AMAZON_ALBUM, AmazonAlbumCache.class, AMAZON_EXPIRATION_TIMEOUT);
	}	
	
	@Override
	public CachedAmazonAlbumData queryExisting(AlbumAndArtist albumAndArtist) {
		Query q;
		
		String album = albumAndArtist.getAlbum();
		String artist = albumAndArtist.getArtist();
		
		q = em.createQuery("FROM CachedAmazonAlbumData album WHERE album.artist = :artist AND album.album = :album");
		q.setParameter("artist", artist.substring(0, Math.min(CachedAmazonAlbumData.DATA_COLUMN_LENGTH, artist.length())));
		q.setParameter("album", album.substring(0, Math.min(CachedAmazonAlbumData.DATA_COLUMN_LENGTH, album.length())));
		
		try {
			return (CachedAmazonAlbumData) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}	
		
	@Override
	protected AmazonAlbumData fetchFromNetImpl(AlbumAndArtist albumAndArtist) {

		AmazonItemSearch search = new AmazonItemSearch(REQUEST_TIMEOUT);
		String amazonKey;
		String amazonAssociateTag;
		final AmazonAlbumData data;
		try {
			amazonKey = config.getPropertyNoDefault(HippoProperty.AMAZON_ACCESS_KEY_ID);
		} catch (PropertyNotFoundException e) {
			amazonKey = null;
		}
		
		try {
			amazonAssociateTag = config.getPropertyNoDefault(HippoProperty.AMAZON_ASSOCIATE_TAG_ID);
			if (amazonAssociateTag.trim().length() == 0)
				amazonAssociateTag = null;
		} catch (PropertyNotFoundException e) {
			amazonAssociateTag = null;
		}
		
		if (amazonKey != null)
			// careful, the artist and album are backward from other apis
			data = search.searchAlbum(amazonKey, amazonAssociateTag, albumAndArtist.getArtist(), albumAndArtist.getAlbum());
		else
			data = null;
		
		return data;
	}

	@Override
	public AmazonAlbumData resultFromEntity(CachedAmazonAlbumData entity) {
		return entity.toData();
	}

	@Override
	public void updateEntityFromResult(AlbumAndArtist key, AmazonAlbumData result, CachedAmazonAlbumData entity) {
		entity.updateData(result);
	}

	@Override
	public CachedAmazonAlbumData entityFromResult(AlbumAndArtist key, AmazonAlbumData result) {
		return new CachedAmazonAlbumData(key, result);
	}

	@Override
	public CachedAmazonAlbumData newNoResultsMarker(AlbumAndArtist key) {
		return CachedAmazonAlbumData.newNoResultsMarker(key);
	}
}
