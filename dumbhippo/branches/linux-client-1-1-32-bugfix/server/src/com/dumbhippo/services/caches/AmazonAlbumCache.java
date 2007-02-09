package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.AmazonAlbumData;

@Local
public interface AmazonAlbumCache extends Cache<AlbumAndArtist,AmazonAlbumData> {

}
