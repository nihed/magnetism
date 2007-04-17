package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.server.listeners.YouTubeListener;

@Local
public interface YouTubeBlockHandler extends ExternalThumbnailedPersonBlockHandler, YouTubeListener {
}
