package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.services.YahooSongDownloadData;

@Local
public interface YahooSongDownloadCache extends AbstractListCache<String,YahooSongDownloadData> {

}
