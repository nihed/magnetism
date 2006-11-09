package com.dumbhippo.services.caches;

import javax.ejb.Local;

@Local
public interface RhapsodyDownloadCache extends AbstractCache<String,Boolean> {

	public String buildLink(String album, String artist, int track);
}
