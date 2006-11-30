package com.dumbhippo.services.caches;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooAlbumSongData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.services.YahooSearchWebServices;
import com.dumbhippo.services.YahooSongData;

@BanFromWebTier
@Stateless
public class YahooAlbumSongsCacheBean
	extends AbstractListCacheBean<String,YahooSongData,CachedYahooAlbumSongData>
	implements YahooAlbumSongsCache {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooAlbumSongsCacheBean.class);

	public YahooAlbumSongsCacheBean() {
		super(Request.YAHOO_ALBUM_SONGS,YahooAlbumSongsCache.class,YAHOO_EXPIRATION_TIMEOUT);
	}
	
	@Override
	public List<CachedYahooAlbumSongData> queryExisting(String albumId) {
		Query q = em.createQuery("SELECT song FROM CachedYahooAlbumSongData song WHERE song.albumId = :albumId");
		q.setParameter("albumId", albumId);
		
		List<CachedYahooAlbumSongData> results = TypeUtils.castList(CachedYahooAlbumSongData.class, q.getResultList());
		return results;
	}

	@Override
	protected List<YahooSongData> fetchFromNetImpl(String albumId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongData> results = ws.lookupAlbumSongs(albumId);
		
		logger.debug("New Yahoo song results from web service for albumId {}: {}", albumId, results);

		return results;
	}

	@Override
	public YahooSongData resultFromEntity(CachedYahooAlbumSongData entity) {
		return entity.toData();
	}

	@Override
	public CachedYahooAlbumSongData entityFromResult(String key, YahooSongData result) {
		if (!result.getAlbumId().equals(key)) {
			// this would break since albumId is the key in the cache and also a returned value in the 
			// song data... if it breaks then we need either the YahooWebServices stuff to filter out
			// these songs, or we need to add a new column to the cache table in the db for the searched-for albumId
			// vs. the returned albumId.
			logger.warn("Yahoo! returned a song with a different albumid than the one in the query for {}: {}", key, result);
		}

		
		CachedYahooAlbumSongData d = new CachedYahooAlbumSongData();
		d.setAlbumId(key);
		d.updateData(result);
		return d;
	}

	@Override
	public CachedYahooAlbumSongData newNoResultsMarker(String key) {
		return CachedYahooAlbumSongData.newNoResultsMarker(key);
	}
}
