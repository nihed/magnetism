package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.services.AmazonAlbumData;

@Local
public interface AmazonAlbumCache extends AbstractCache<AlbumAndArtist,AmazonAlbumData> {

}
