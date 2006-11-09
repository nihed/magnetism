package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.services.YahooSongData;

@Local
public interface YahooAlbumSongsCache extends AbstractListCache<String,YahooSongData> {

}
