package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.services.YahooAlbumData;

@Local
public interface YahooArtistAlbumsCache {
    public List<YahooAlbumData> getSync(String artistId);
	
	public Future<List<YahooAlbumData>> getAsync(String artistId);
	
	public List<YahooAlbumData> checkCache(String artistId);
	
	public List<YahooAlbumData> fetchFromNet(String artistId);
	
	public List<YahooAlbumData> saveInCache(String artistId, List<YahooAlbumData> songs);
}
