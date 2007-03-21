package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.PicasaUpdateStatus;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.services.PicasaAlbum;

/** 
 * This bean caches Picasa video feeds.
 * 
 * @author Colin Walters
 */
@Local
public interface PicasaUpdater extends CachedExternalUpdater<PicasaUpdateStatus>, PollingTaskLoader {	
	public boolean saveUpdatedStatus(String username, List<? extends PicasaAlbum> videos);
}
