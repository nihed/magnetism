package com.dumbhippo.server;

import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.services.YahooAlbumData;

@Local
public interface YahooAlbumCache {
	
	public YahooAlbumData getSync(String albumId); 
	
	public Future<YahooAlbumData> getAsync(String albumId);
	
	public YahooAlbumData checkCache(String albumId);
	
	public YahooAlbumData fetchFromNet(String albumId);
	
	public YahooAlbumData saveInCache(String albumId, YahooAlbumData data);
}
