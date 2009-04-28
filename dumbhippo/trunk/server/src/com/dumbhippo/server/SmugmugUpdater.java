package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.SmugmugUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.services.smugmug.rest.bind.Image;


/** 
 * This bean caches Smugmug video feeds.
 */
@Local
public interface SmugmugUpdater extends CachedExternalUpdater<SmugmugUpdateStatus>, PollingTaskLoader {	
	public boolean saveUpdatedStatus(String username, List<? extends Image> videos);
}
