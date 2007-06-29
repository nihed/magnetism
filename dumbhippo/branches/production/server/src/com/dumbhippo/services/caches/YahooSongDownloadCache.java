package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.YahooSongDownloadData;

@Local
public interface YahooSongDownloadCache extends ListCache<String,YahooSongDownloadData> {

}
