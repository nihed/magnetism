package com.dumbhippo.services;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.YahooSongDownloadResult;
import com.dumbhippo.persistence.YahooSongResult;

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
