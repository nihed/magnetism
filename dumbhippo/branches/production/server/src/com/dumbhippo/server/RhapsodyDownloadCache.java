package com.dumbhippo.server;

import java.util.concurrent.Future;

import javax.ejb.Local;

@Local
public interface RhapsodyDownloadCache {
	
	public String getSync(String album, String artist, int track); 
	
	public Future<String> getAsync(String album, String artist, int track);
	
	public Boolean checkCache(String link);
	
	public boolean fetchFromNet(String link);
	
	public String saveInCache(String link, boolean valid);
}
