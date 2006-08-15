package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.services.YahooSongData;

@Local
public interface YahooSongCache {
	public List<YahooSongData> getSync(Track track);
	
	public Future<List<YahooSongData>> getAsync(Track track);
	
	public List<YahooSongData> checkCache(Track track);
	
	public List<YahooSongData> fetchFromNet(Track track);
	
	public List<YahooSongData> saveInCache(Track track, List<YahooSongData> songs);
}
