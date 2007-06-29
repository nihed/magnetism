package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.YahooSongData;

@Local
public interface YahooAlbumSongsCache extends ListCache<String,YahooSongData> {

}
