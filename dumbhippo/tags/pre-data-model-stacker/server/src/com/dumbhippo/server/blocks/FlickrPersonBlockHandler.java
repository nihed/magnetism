package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.FlickrListener;

@Local
public interface FlickrPersonBlockHandler extends ExternalThumbnailedPersonBlockHandler, FlickrListener {
}
