package com.dumbhippo.server.downloads;

public class DownloadPlatform {
	private String name;
	private String version;
	private String minimum;
	private String date;
	
	DownloadPlatform(String name, String version, String minimum, String date) {
		this.name = name;
		this.version = version;
		this.minimum = minimum;
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getMinimum() {
		return minimum;
	}

	public String getDate() {
		return date;
	}
}
