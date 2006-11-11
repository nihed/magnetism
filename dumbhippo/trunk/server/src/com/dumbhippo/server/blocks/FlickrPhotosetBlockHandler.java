package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.FlickrPhotosetStatus;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.FlickrListener;

@Local
public interface FlickrPhotosetBlockHandler extends BlockHandler, FlickrListener {
	public BlockKey getKey(User user, FlickrPhotosetStatus photosetStatus);
}
