package com.dumbhippo.persistence;

public enum SongDownloadSource {
	ITUNES("iTunes"),
	YAHOO("Yahoo! Music Unlimited"),
	RHAPSODY("Rhapsody"),
	//RHAPSODY_PARTNER("Rhapsody Partner"),  // this would capture "legacy Rhapsody" results from Yahoo
	// this is used to create a db row indicating that yahoo returned no results
	NONE_MARKER("[not found in yahoo]");
	
	private String yahooSourceName;
	
	private SongDownloadSource(String yahooSourceName) {
		this.yahooSourceName = yahooSourceName;
	}
	
	// name that's in the yahoo web services xml as &lt;Source&gt;
	public String getYahooSourceName() {
		return yahooSourceName;
	}
	
	public static SongDownloadSource parseYahooSourceName(String name) {
		for (SongDownloadSource s : SongDownloadSource.values()) {
			if (name.equals(s.getYahooSourceName()))
				return s;
		}
		return null;
	}
}
