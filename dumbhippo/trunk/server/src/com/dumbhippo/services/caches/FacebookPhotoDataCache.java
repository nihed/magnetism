package com.dumbhippo.services.caches;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.FacebookPhotoDataStatus;
import com.dumbhippo.services.FacebookPhotoDataView;

@Local
public interface FacebookPhotoDataCache extends ListCache<String, FacebookPhotoDataView> {
	
	/**
	 * Returned cached photos for a given user that match specified photo data statuses.
	 * 
	 * @param key a mugshot user id
	 * @param photos photo data statuses to find matching cached photos for
	 * @return FacebookPhotoDataView objects that have information about the photos
	 */
	public List<FacebookPhotoDataView> queryExisting(String key, Set<FacebookPhotoDataStatus> photos);

}