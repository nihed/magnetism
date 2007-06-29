package com.dumbhippo.services.caches;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedYahooArtistAlbumData;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.services.YahooAlbumData;
import com.dumbhippo.services.YahooSearchWebServices;

@BanFromWebTier
//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class YahooArtistAlbumsCacheBean
	extends AbstractListCacheWithStorageBean<String,YahooAlbumData,CachedYahooArtistAlbumData>
	implements YahooArtistAlbumsCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(YahooArtistAlbumsCacheBean.class);
	
	public YahooArtistAlbumsCacheBean() {
		super(Request.YAHOO_ARTIST_ALBUMS, YahooArtistAlbumsCache.class, YAHOO_EXPIRATION_TIMEOUT, YahooAlbumData.class);
	}
	
	@Override
	public List<CachedYahooArtistAlbumData> queryExisting(String artistId) {
		Query q = em.createQuery("SELECT album FROM CachedYahooArtistAlbumData album WHERE album.artistId = :artistId");
		q.setParameter("artistId", artistId);
		
		List<CachedYahooArtistAlbumData> results = TypeUtils.castList(CachedYahooArtistAlbumData.class, q.getResultList());
		return results;
	}

	@Override
	protected List<YahooAlbumData> fetchFromNetImpl(String artistId) {
		YahooSearchWebServices ws = new YahooSearchWebServices(REQUEST_TIMEOUT, config);
		List<YahooAlbumData> results = ws.lookupAlbumsByArtistId(artistId);
		
		logger.debug("New Yahoo album results from web service for artistId {}: {}", artistId, results);

		return results;
	}

	private CachedYahooArtistAlbumData createCachedAlbum(String artistId) {
		CachedYahooArtistAlbumData d = new CachedYahooArtistAlbumData();
		
		// This is both the lookup key and part of the returned results. 
		// Yahoo always returns the album with artistId = the artistId 
		// you searched for, though, so we don't need a separate column
		// for the artist id of the search vs. of the album.
		d.setArtistId(artistId);
		return d;
	}	

	@Override
	public YahooAlbumData resultFromEntity(CachedYahooArtistAlbumData entity) {
		return entity.toData();
	}

	@Override
	public CachedYahooArtistAlbumData entityFromResult(String key, YahooAlbumData result) {
		CachedYahooArtistAlbumData d = createCachedAlbum(key);
		d.updateData(key, result);
		return d;
	}

	@Override
	public CachedYahooArtistAlbumData newNoResultsMarker(String key) {
		return CachedYahooArtistAlbumData.newNoResultsMarker(key);
	}

	@Override
	public void setAllLastUpdatedToZero(String key) {
		throw new UnsupportedOperationException("expiring this cache not supported");
	}
	
	private Query buildSearchQuery(String artist, String album, String name, int maxResults) throws NotFoundException {		
		int count = 0;
		if (artist != null)
			++count;
		if (name != null)
			++count;
		if (album != null)
			++count;
		
		if (count == 0)
			throw new NotFoundException("Search has no parameters");
		
		StringBuilder sb = new StringBuilder("SELECT album FROM CachedYahooArtistAlbumData album");
		if (name != null) {
			sb.append(", CachedYahooAlbumSongData song ");
		}
		sb.append(" WHERE ");
		if (artist != null) {
			sb.append(" album.artist = :artist ");
			--count;
			if (count > 0)
				sb.append(" AND ");
		}
		if (album != null) {
			sb.append(" album.album = :album ");
			--count;
			if (count > 0)
				sb.append(" AND ");
		}
		if (name != null) {
			sb.append(" song.name = :name AND album.albumId = song.albumId ");
			--count;
		}		
		if (count != 0)
			throw new RuntimeException("broken code in song search");		    	

		// we want to make this deterministic, so that we always return the same 
		// album for the same artist-album-song combination, we always use the 
		// same (earliest-created) row
		sb.append(" ORDER BY album.id");
		
		Query q = em.createQuery(sb.toString());
		if (artist != null)
			q.setParameter("artist", artist);
		if (album != null)
			q.setParameter("album", album);
		if (name != null)
			q.setParameter("name", name);
		
		if (maxResults >= 0)
			q.setMaxResults(maxResults);

		return q;
	}	
	
	public YahooAlbumData findAlreadyCachedAlbum(String artist, String album, String song) throws NotFoundException {
		Query q = buildSearchQuery(artist, album, song, 1);
		try {
			CachedYahooArtistAlbumData result = (CachedYahooArtistAlbumData) q.getSingleResult();
			if (result.isNoResultsMarker())
				throw new NotFoundException("Search returned no results marker (should this happen?)");
			return result.toData();
		} catch (NoResultException e) {
			throw new NotFoundException("search did not match anything in the database");
		}
	}
}
