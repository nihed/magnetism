package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.YouTubeVideo;

@Local
public interface YouTubeVideosCache extends AbstractListCache<String,YouTubeVideo> {
}
