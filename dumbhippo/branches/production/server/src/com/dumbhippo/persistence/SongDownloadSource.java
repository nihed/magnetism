package com.dumbhippo.persistence;

public enum SongDownloadSource {
	ITUNES("iTunes", "iTunes"),
	YAHOO("Yahoo! Music Unlimited", "Yahoo! Music"),
	RHAPSODY("Rhapsody", "Rhapsody"),
	//RHAPSODY_PARTNER("Rhapsody Partner"),  // this would capture "legacy Rhapsody" results from Yahoo
	// this is used to create a db row indicating that yahoo returned no results
	NONE_MARKER("[not found in yahoo]", null);
	
	private String yahooSourceName;
	private String shortName;
	
	private SongDownloadSource(String yahooSourceName, String shortName) {
		this.yahooSourceName = yahooSourceName;
		this.shortName = shortName;
	}
	
	// name that's in the yahoo web services xml as &lt;Source&gt;
	public String getYahooSourceName() {
		return yahooSourceName;
	}
	
	// name for display on the web
	public String getShortName() {
		return shortName;
	}
	
	public static SongDownloadSource parseYahooSourceName(String name) {
		for (SongDownloadSource s : SongDownloadSource.values()) {
			if (name.equals(s.getYahooSourceName()))
				return s;
		}
		return null;
	}
}
