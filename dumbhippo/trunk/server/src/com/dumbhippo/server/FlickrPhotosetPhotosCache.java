package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.services.FlickrPhotoView;

@Local
public interface FlickrPhotosetPhotosCache extends AbstractListCache<String,FlickrPhotoView> {

}
