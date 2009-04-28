package com.dumbhippo.server.listeners;

import java.util.List;

import com.dumbhippo.services.smugmug.rest.bind.Image;

public interface SmugmugListener {
	public void onSmugmugRecentAlbumsChanged(String username, List<? extends Image> albums);
}
