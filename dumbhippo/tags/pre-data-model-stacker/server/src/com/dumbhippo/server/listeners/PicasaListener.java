package com.dumbhippo.server.listeners;

import java.util.List;

import com.dumbhippo.services.PicasaAlbum;

public interface PicasaListener {
	public void onPicasaRecentAlbumsChanged(String username, List<? extends PicasaAlbum> albums);
}
