package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.persistence.CachedFacebookPhotoData;

@Local
public interface FacebookPhotoDataCache extends ListCache<String, CachedFacebookPhotoData> {

}