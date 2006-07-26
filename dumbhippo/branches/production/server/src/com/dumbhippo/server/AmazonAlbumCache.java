package com.dumbhippo.server;

import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.services.AmazonAlbumData;

@Local
public interface AmazonAlbumCache {
	
	public AmazonAlbumData getSync(String album, String artist); 
	
	public Future<AmazonAlbumData> getAsync(String album, String artist);
	
	public AmazonAlbumData checkCache(String album, String artist);
	
	public AmazonAlbumData fetchFromNet(String album, String artist);
	
	public AmazonAlbumData saveInCache(String album, String artist, AmazonAlbumData data);
}
