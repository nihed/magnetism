package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.services.YahooAlbumData;

@Local
public interface YahooArtistAlbumsCache extends AbstractListCache<String,YahooAlbumData> {
	
	public YahooAlbumData findAlreadyCachedAlbum(String artist, String album, String song) throws NotFoundException;
}
