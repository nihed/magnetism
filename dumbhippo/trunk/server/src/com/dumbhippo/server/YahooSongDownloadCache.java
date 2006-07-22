package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.YahooSongDownloadResult;

@Local
public interface YahooSongDownloadCache {
	
	public List<YahooSongDownloadResult> getYahooSongDownloadResultsSync(String songId);
	
	public Future<List<YahooSongDownloadResult>> getYahooSongDownloadResultsAsync(String songId);

}
