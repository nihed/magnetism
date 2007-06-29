package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.LastFmUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.services.LastFmTrack;

/** 
 * This bean synchronizes Last.fm music updates with our TrackHistory.
 * 
 * @author Colin Walters
 */
@Local
public interface LastFmUpdater extends CachedExternalUpdater<LastFmUpdateStatus>, PollingTaskLoader {
	public boolean saveUpdatedStatus(String username, List<LastFmTrack> tracks);
}
