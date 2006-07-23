package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooArtistResult;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.impl.ConfigurationBean;

public class YahooSearchWebServices extends AbstractXmlRequest<YahooSearchSaxHandler> {

	static private final Logger logger = GlobalSetup.getLogger(YahooSearchWebServices.class);

	// 50 is the maximum number of results we can get with one request, 10 is the default number
	static public final int maxResultsToReturn = 50;

	private String appId;
	
	public YahooSearchWebServices(int timeoutMilliseconds, Configuration config) {
		super(timeoutMilliseconds);
		try {
			this.appId = config.getPropertyNoDefault(HippoProperty.YAHOO_APP_ID);
			if (appId.trim().length() == 0)
				appId = null;
		} catch (PropertyNotFoundException e) {
			appId = null;
		}

		if (appId == null)
			logger.warn("Yahoo! app ID is not set, can't make Yahoo! web services calls.");
	}
	
	/** 
	 * Pass null or -1 for any missing fields.
	 * 
	 * @param artist
	 * @param album
	 * @param name
	 * @returns list of results (possibly empty)
	 */
	public List<YahooSongData> lookupSong(String artist, String album, String name) {
		
		if (artist == null || album == null || name == null) {
			logger.debug("one of artist/album/name missing, can't do yahoo search on this track");
			return Collections.emptyList();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/songSearch?results=3&appid=");
		sb.append(appId);
		sb.append("&artist=");
		sb.append(StringUtils.urlEncode(artist));
		sb.append("&album=");
		sb.append(StringUtils.urlEncode(album));
		sb.append("&song=");
		sb.append(StringUtils.urlEncode(name));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo song search {}", wsUrl);
		
		// we could double-check that the song result is the right one, but 
		// really it seems more harmful to reject some correct results
		// than include some wrong ones
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.error("Song search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getBestSongs(artist, album, name);
		}
	}
	
	public List<YahooSongData> lookupAlbumSongs(String album, String artist) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/songSearch?results=50&appid=");
		sb.append(appId);
		sb.append("&artist=");
		sb.append(StringUtils.urlEncode(artist));
		sb.append("&album=");
		sb.append(StringUtils.urlEncode(album));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo album songs search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.error("Album songs search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getAlbumSongs();
		}		
	}

	public List<YahooSongData> lookupAlbumSongs(String albumId) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/songSearch?results=50&appid=");
		sb.append(appId);
		sb.append("&albumid=");
		sb.append(StringUtils.urlEncode(albumId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo album songs search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.error("Album songs search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getAlbumSongs();
		}		
	}
	
	public Pair<Integer, List<YahooAlbumResult>> lookupAlbums(String artistId, 
			                                                 int start,
			                                                 int resultsToReturn) {		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/albumSearch?appid=");
		sb.append(appId);
		sb.append("&start=");
		sb.append(StringUtils.urlEncode(Integer.toString(start)));
		sb.append("&results=");
		sb.append(StringUtils.urlEncode(Integer.toString(resultsToReturn)));
		sb.append("&artistid=");		
		sb.append(StringUtils.urlEncode(artistId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo album search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.error("Album search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			return new Pair<Integer, List<YahooAlbumResult>>(-1, new ArrayList<YahooAlbumResult>());
		} else {
			return new Pair<Integer, List<YahooAlbumResult>>(handler.getTotalResultsAvailable(), handler.getBestAlbums());
		}
	}
	
	public YahooAlbumResult lookupAlbum(String albumId) throws NotFoundException {		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/albumSearch?appid=");
		sb.append(appId);
		sb.append("&results=1");
		sb.append("&albumid=");		
		sb.append(StringUtils.urlEncode(albumId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo album search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.error("Album search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			throw new NotFoundException("Album search failed, returning nothing");
		} else {
            List<YahooAlbumResult> albums = handler.getBestAlbums();
            if (albums.isEmpty()) {
            	throw new NotFoundException("No albums matching albumId " + albumId + " were found.");
            }
            // we requested 1 result, so there shouldn't be more than 1 result
            return albums.get(0);
		}
	}
	
	public List<YahooSongDownloadData> lookupDownloads(String songId) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/songDownloadLocation?appid=");
		sb.append(appId);
		sb.append("&songid=");
		sb.append(StringUtils.urlEncode(songId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo song download search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(songId), wsUrl);
		if (handler == null) {
			logger.error("Download search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getBestDownloads();
		}
	}

	public List<YahooArtistResult> lookupArtist(String artist, String artistId) {
		
		StringBuilder sb = new StringBuilder();
		// because we are not doing much with an artist id for now, one result is all we need,
		// eventually, we might use more results that would provide us with different ids (using
		// all of which we could lookup more albums), more results would also include results where
		// multiple artists are performing together, such as "Miles Davis Quintet" or 
		// "Miles Davis & John Coltrane", when searching for "Miles Davis"
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/artistSearch?results=1&appid=");
		sb.append(appId);
		// if artistId is not null, then use artistId only
		if (artistId != null) {
		    sb.append("&artistid=");
		    sb.append(StringUtils.urlEncode(artistId));
		} else {
			// if artistId was null, artist has to be not null 
		    sb.append("&artist=");
		    sb.append(StringUtils.urlEncode(artist));			
		}
		
		String wsUrl = sb.toString();
		logger.debug("Loading yahoo artist search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.error("Artist search failed, it is possible that Yahoo rate limit was exceeded, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getBestArtists();
		}
	}
	
	static public final void main(String[] args) {
		ConfigurationBean config = new ConfigurationBean();
		config.init();
		
		YahooSearchWebServices ws = new YahooSearchWebServices(6000, config);
		List<YahooSongData> list = ws.lookupSong("Bob Dylan", "Time Out of Mind", "Tryin' To Get To Heaven");
		for (YahooSongData r : list) {
			System.out.println("Got result: " + r);
			List<YahooSongDownloadData> listD = ws.lookupDownloads(r.getSongId());
			for (YahooSongDownloadData d : listD) {
				System.out.println("  download: " + d);
			}
		}
	}
}
