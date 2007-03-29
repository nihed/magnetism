package com.dumbhippo.server.downloads;

import java.util.ArrayList;
import java.util.List;

public class DownloadDistribution {
	private DownloadPlatform platform;
	private String name;
	private String osVersion;
	private String osVersionPattern;
	private String release;
	private List<Download> downloads = new ArrayList<Download>();
	
	public DownloadDistribution(DownloadPlatform platform) {
		this.platform = platform;
	}
	
	public DownloadPlatform getPlatform() {
		return platform;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getOsVersion() {
		return osVersion;
	}
	
	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}
	
	public String getOsVersionPattern() {
		return osVersionPattern;
	}
	
	public void setOsVersionPattern(String osVersionPattern) {
		this.osVersionPattern = osVersionPattern;
	}
	
	public String getRelease() {
		return release;
	}
	
	public void setRelease(String release) {
		this.release = release;
	}

	public List<Download> getDownloads() {
		return downloads;
	}
	
	public void addDownload(Download download) {
		downloads.add(download);
	}
}
