package com.dumbhippo.server;

import java.util.concurrent.Future;

import javax.ejb.Local;

import com.dumbhippo.persistence.AmazonAlbumResult;

@Local
public interface AmazonAlbumCache {
	
	public AmazonAlbumResult getAmazonAlbumSync(final String album, final String artist); 
	
	public Future<AmazonAlbumResult> getAmazonAlbumAsync(String album, String artist);

}
