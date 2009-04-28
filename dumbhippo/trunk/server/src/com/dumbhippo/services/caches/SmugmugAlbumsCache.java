package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.smugmug.rest.bind.Image;

@Local
public interface SmugmugAlbumsCache extends ListCache<String,Image> {
}
