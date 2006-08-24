package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.services.YahooSongData;

@Local
public interface YahooAlbumSongsCache {
    public List<YahooSongData> getSync(String albumId);
	
	public Future<List<YahooSongData>> getAsync(String albumId);
	
	public List<YahooSongData> checkCache(String albumId);
	
	public List<YahooSongData> fetchFromNet(String albumId);
	
	public List<YahooSongData> saveInCache(String albumId, List<YahooSongData> songs);
}
