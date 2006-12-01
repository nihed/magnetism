package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.FlickrPhotosView;

@Local
public interface FlickrUserPhotosCache extends Cache<String,FlickrPhotosView> {
}
