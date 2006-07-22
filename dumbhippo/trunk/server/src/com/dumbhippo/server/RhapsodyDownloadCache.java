package com.dumbhippo.server;

import javax.ejb.Local;

@Local
public interface RhapsodyDownloadCache {
	/**
	 * Try to find a Rhapsody friendly URL for this song.
	 * 
	 * @param songId
	 * @param artistName
	 * @param albumName
	 * @param trackNumber
	 * @return a working download url result, or null otherwise
	 */
	public String getRhapsodyDownloadUrl(String artistName, String albumName, int trackNumber);
}
