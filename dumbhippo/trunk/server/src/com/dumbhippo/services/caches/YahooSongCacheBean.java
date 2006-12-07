package com.dumbhippo.services.caches;

import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooSongData;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.services.YahooSearchWebServices;
import com.dumbhippo.services.YahooSongData;

@TransactionAttribute(TransactionAttributeType.REQUIRED) // because the base classes change the default; not sure this is needed, but can't hurt
@BanFromWebTier
@Stateless
public class YahooSongCacheBean
	extends AbstractListCacheWithStorageBean<Track,YahooSongData,CachedYahooSongData>
	implements YahooSongCache {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooSongCacheBean.class);

	public YahooSongCacheBean() {
		super(Request.YAHOO_SONG, YahooSongCache.class, YAHOO_EXPIRATION_TIMEOUT, YahooSongData.class);
	}

	@Override
	public List<CachedYahooSongData> queryExisting(Track track) {
		Query q = em.createQuery("SELECT song FROM CachedYahooSongData song WHERE song.searchedName = :name AND song.searchedArtist = :artist AND song.searchedAlbum = :album");
		q.setParameter("name", track.getName());
		q.setParameter("artist", track.getArtist());
		q.setParameter("album", track.getAlbum());
		
		List<CachedYahooSongData> results = TypeUtils.castList(CachedYahooSongData.class, q.getResultList());
		return results;
	}

	@Override
	public List<? extends YahooSongData> checkCache(Track track) throws NotCachedException {
		// yahoo lookup requires these fields, so just never do a web request without them
		if (track.getArtist() == null ||
			track.getAlbum() == null ||
			track.getName() == null) {
			logger.debug("Track {} missing artist album or name, can't get yahoo stuff", track);
			List<YahooSongData> empty = Collections.emptyList();
			return empty;
		} else {
			return super.checkCache(track);
		}
	}

	@Override
	protected List<YahooSongData> fetchFromNetImpl(Track track) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooSongData> results = ws.lookupSong(track.getArtist(), track.getAlbum(), track.getName());
		
		logger.debug("New Yahoo song results from web service for track {}: {}", track, results);

		return results;
	}

	private CachedYahooSongData createCachedSong(Track track) {
		CachedYahooSongData d = new CachedYahooSongData();
		d.setSearchedAlbum(track.getAlbum());
		d.setSearchedArtist(track.getArtist());
		d.setSearchedName(track.getName());
		return d;
	}
	
	@Override
	public YahooSongData resultFromEntity(CachedYahooSongData entity) {
		return entity.toData();
	}

	@Override
	public CachedYahooSongData entityFromResult(Track key, YahooSongData result) {
		CachedYahooSongData d = createCachedSong(key);
		d.updateData(result);
		return d;
	}

	@Override
	public CachedYahooSongData newNoResultsMarker(Track key) {
		return CachedYahooSongData.newNoResultsMarker(key.getArtist(),
				key.getAlbum(), key.getName());
	}
}
