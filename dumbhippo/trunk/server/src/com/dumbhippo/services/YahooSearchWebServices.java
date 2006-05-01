package com.dumbhippo.services;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.YahooAlbumResult;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;
import com.dumbhippo.server.NotFoundException;

public class YahooSearchWebServices extends AbstractXmlRequest<YahooSearchSaxHandler> {

	static private final Logger logger = GlobalSetup.getLogger(YahooSearchWebServices.class);
	
	private String appId;
	
	public YahooSearchWebServices(int timeoutMilliseconds) {
		super(timeoutMilliseconds);
		this.appId = "dumbhippo";
	}
	
	/** 
	 * Pass null or -1 for any missing fields.
	 * 
	 * @param artist
	 * @param album
	 * @param name
	 * @param duration
	 * @param trackNumber
	 * @returns list of results (possibly empty)
	 */
	public List<YahooSongResult> lookupSong(String artist, String album, String name,
			int duration, int trackNumber) {
		
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
			logger.debug("Song search failed, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getBestSongs();
		}
	}
	
	public List<YahooSongResult> lookupAlbumSongs(String album, String artist) {
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
			logger.debug("Album songs search failed, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getAlbumSongs();
		}		
	}

	public List<YahooSongResult> lookupAlbumSongs(String albumId) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/songSearch?results=50&appid=");
		sb.append(appId);
		sb.append("&albumid=");
		sb.append(StringUtils.urlEncode(albumId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo album songs search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.debug("Album songs search failed, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getAlbumSongs();
		}		
	}
	
	public List<YahooAlbumResult> lookupAlbums(String artistId) {		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/albumSearch?appid=");
		sb.append(appId);
		// 50 is the maximum number of results we can get with one request, 10 is the default 
		// number
		sb.append("&results=50");
		sb.append("&artistid=");		
		sb.append(StringUtils.urlEncode(artistId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo album search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.debug("Album search failed, returning nothing");
			return Collections.emptyList();
		} else {
			return handler.getBestAlbums();
		}
	}
	
	public List<YahooSongDownloadResult> lookupDownloads(String songId) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/songDownloadLocation?appid=");
		sb.append(appId);
		sb.append("&songid=");
		sb.append(StringUtils.urlEncode(songId));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo song download search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.debug("Download search failed, returning nothing");
			return Collections.emptyList();
		} else {
			List<YahooSongDownloadResult> list = handler.getBestDownloads();
			for (YahooSongDownloadResult result : list) {
				result.setSongId(songId);
			}
			return list;
		}
	}

	public String lookupArtistId(String artist) throws NotFoundException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("http://api.search.yahoo.com/AudioSearchService/V1/artistSearch?appid=");
		sb.append(appId);
		sb.append("&artist=");
		sb.append(StringUtils.urlEncode(artist));

		String wsUrl = sb.toString();
		logger.debug("Loading yahoo artist search {}", wsUrl);
		
		YahooSearchSaxHandler handler = parseUrl(new YahooSearchSaxHandler(), wsUrl);
		if (handler == null) {
			logger.debug("Artist Id search failed, returning nothing");
			throw new NotFoundException("Artist Id search failed, returning nothing");
		} else {
			return handler.getSingleArtistId();
		}
	}
	
	static public final void main(String[] args) {
		YahooSearchWebServices ws = new YahooSearchWebServices(6000);
		List<YahooSongResult> list = ws.lookupSong("Bob Dylan", "Time Out of Mind", "Tryin' To Get To Heaven", -1, -1);
		for (YahooSongResult r : list) {
			System.out.println("Got result: " + r);
			List<YahooSongDownloadResult> listD = ws.lookupDownloads(r.getSongId());
			for (YahooSongDownloadResult d : listD) {
				System.out.println("  download: " + d);
			}
		}
	}
}
