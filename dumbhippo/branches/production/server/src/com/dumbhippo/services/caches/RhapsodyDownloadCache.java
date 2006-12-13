package com.dumbhippo.services.caches;

import java.util.List;

import javax.ejb.Local;

@Local
public interface RhapsodyDownloadCache extends Cache<String,Boolean> {

	public List<String> buildLinks(String album, String artist, String name);
}
