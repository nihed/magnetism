package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.FlickrPhotosetView;

@Local
public interface FlickrUserPhotosetsCache extends AbstractListCache<String,FlickrPhotosetView> {

}
