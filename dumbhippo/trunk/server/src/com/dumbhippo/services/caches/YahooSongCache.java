package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.services.YahooSongData;

@Local
public interface YahooSongCache extends AbstractListCache<Track,YahooSongData> {

}
