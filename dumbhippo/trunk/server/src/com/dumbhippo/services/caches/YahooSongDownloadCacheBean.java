package com.dumbhippo.services.caches;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooSongDownload;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.services.YahooSearchWebServices;
import com.dumbhippo.services.YahooSongDownloadData;

@BanFromWebTier
@Stateless
public class YahooSongDownloadCacheBean
	extends AbstractListCacheWithStorageBean<String,YahooSongDownloadData,CachedYahooSongDownload>
	implements YahooSongDownloadCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSongDownloadCacheBean.class);
	
	public YahooSongDownloadCacheBean() {
		super(Request.YAHOO_SONG_DOWNLOAD, YahooSongDownloadCache.class, YAHOO_EXPIRATION_TIMEOUT, YahooSongDownloadData.class);
	}
	
	@Override
	public List<CachedYahooSongDownload> queryExisting(String songId) {
		Query q = em.createQuery("SELECT song FROM CachedYahooSongDownload song WHERE song.songId = :songId");
		q.setParameter("songId", songId);
		
		List<CachedYahooSongDownload> results = TypeUtils.castList(CachedYahooSongDownload.class, q.getResultList());
		return results;
	}

	@Override
	protected List<YahooSongDownloadData> fetchFromNetImpl(String songId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongDownloadData> results = ws.lookupDownloads(songId);
		
		logger.debug("New Yahoo download results from web service for songId {}: {}", songId, results);

		return results;
	}

	@Override
	public YahooSongDownloadData resultFromEntity(CachedYahooSongDownload entity) {
		return entity.toData();
	}

	@Override
	public CachedYahooSongDownload entityFromResult(String key, YahooSongDownloadData result) {
		CachedYahooSongDownload d = new CachedYahooSongDownload();
		d.setSongId(key);
		d.updateData(result);
		return d;
	}

	@Override
	public CachedYahooSongDownload newNoResultsMarker(String key) {
		return CachedYahooSongDownload.newNoResultsMarker(key);
	}
}
