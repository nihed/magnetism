package com.dumbhippo.services;

import com.dumbhippo.persistence.SongDownloadSource;

public interface YahooSongDownloadData {
	public String getSongId();
	
	public String getUrl();
	
	public String getFormats();
	
	public String getPrice();
	
	public String getRestrictions();

	public SongDownloadSource getSource();
}
