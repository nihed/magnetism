package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.YahooSongResult;

@Local
public interface YahooSongCache {
	public List<YahooSongResult> getYahooSongResultsSync(Track track);
	
    public List<YahooSongResult> getYahooSongResultsSync(String albumId);
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(Track track);
	
	public Future<List<YahooSongResult>> getYahooSongResultsAsync(String albumId);
}
