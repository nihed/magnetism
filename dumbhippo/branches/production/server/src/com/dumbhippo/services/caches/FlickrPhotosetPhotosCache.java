package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.FlickrPhotoView;

@Local
public interface FlickrPhotosetPhotosCache extends ListCache<String,FlickrPhotoView> {

}
