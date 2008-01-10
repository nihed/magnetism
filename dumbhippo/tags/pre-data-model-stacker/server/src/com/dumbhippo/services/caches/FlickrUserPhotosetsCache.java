package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.FlickrPhotosetsView;

@Local
public interface FlickrUserPhotosetsCache extends Cache<String,FlickrPhotosetsView> {

}
