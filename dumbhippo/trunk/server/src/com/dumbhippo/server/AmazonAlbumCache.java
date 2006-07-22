package com.dumbhippo.server;

import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.AmazonAlbumResult;
import com.dumbhippo.services.AmazonAlbumData;

@Local
public interface AmazonAlbumCache {
	
	public AmazonAlbumResult getSync(String album, String artist); 
	
	public Future<AmazonAlbumResult> getAsync(String album, String artist);
	
	public AmazonAlbumResult checkCache(String album, String artist);
	
	public AmazonAlbumData fetchFromNet(String album, String artist);
	
	public AmazonAlbumResult saveInCache(String album, String artist, AmazonAlbumData data);
}
