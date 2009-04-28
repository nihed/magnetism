package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.SmugmugListener;

@Local
public interface SmugmugBlockHandler extends ExternalThumbnailedPersonBlockHandler, SmugmugListener {
}
