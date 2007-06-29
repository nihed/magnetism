package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.services.YahooAlbumData;

@Local
public interface YahooArtistAlbumsCache extends ListCache<String,YahooAlbumData> {
	
	public YahooAlbumData findAlreadyCachedAlbum(String artist, String album, String song) throws NotFoundException;
}
