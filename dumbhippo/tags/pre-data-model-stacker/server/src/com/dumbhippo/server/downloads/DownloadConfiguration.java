package com.dumbhippo.server.downloads;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.server.NotFoundException;

public class DownloadConfiguration {
	private List<DownloadPlatform> platforms = new ArrayList<DownloadPlatform>();
	
	public DownloadConfiguration() {
	}
	
	public void addPlatform(DownloadPlatform platform) {
		platforms.add(platform);
	}

	public Download findDownload(String platformName, String distributionName, String osVersion, String architecture) throws NotFoundException {
		for (DownloadPlatform platform : platforms) {
			for (DownloadDistribution distribution : platform.getDistributions()) {
				for (Download download : distribution.getDownloads()) {
					if (download.matches(platformName, distributionName, osVersion, architecture))
						return download;
				}
			}
		}
		
		throw new NotFoundException("No matching download found");
	}
	
	public DownloadPlatform findPlatform(String platformName) throws NotFoundException {
		for (DownloadPlatform platform : platforms) {
			if (platform.getName().equals(platformName))
				return platform;
		}
		
		throw new NotFoundException("No platform named '" + platformName + "'");
	}
}
