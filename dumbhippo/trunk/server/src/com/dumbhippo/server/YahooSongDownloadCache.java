package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.services.YahooSongDownloadData;

@Local
public interface YahooSongDownloadCache {
	
    public List<YahooSongDownloadData> getSync(String songId);
	
	public Future<List<YahooSongDownloadData>> getAsync(String songId);
	
	public List<YahooSongDownloadData> checkCache(String songId);
	
	public List<YahooSongDownloadData> fetchFromNet(String songId);
	
	public List<YahooSongDownloadData> saveInCache(String songId, List<YahooSongDownloadData> songs);
}
