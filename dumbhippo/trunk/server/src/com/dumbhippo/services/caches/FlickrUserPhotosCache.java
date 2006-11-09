package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.FlickrPhotoView;

@Local
public interface FlickrUserPhotosCache extends AbstractListCache<String,FlickrPhotoView> {
}
