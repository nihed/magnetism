package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.PicasaListener;

@Local
public interface PicasaBlockHandler extends ExternalThumbnailedPersonBlockHandler, PicasaListener {
}
