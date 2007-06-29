package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.PicasaAlbum;

@Local
public interface PicasaAlbumsCache extends ListCache<String,PicasaAlbum> {
}
