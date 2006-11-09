package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.YahooAlbumData;

@Local
public interface YahooAlbumCache extends AbstractCache<String,YahooAlbumData> {

}
