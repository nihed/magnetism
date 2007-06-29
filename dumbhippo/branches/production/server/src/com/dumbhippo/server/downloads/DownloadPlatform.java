package com.dumbhippo.server.downloads;

import java.util.ArrayList;
import java.util.List;

public class DownloadPlatform {
	private String name;
	private String version;
	private String minimum;
	private String date;
	private List<DownloadDistribution> distributions = new ArrayList<DownloadDistribution>();
	
	DownloadPlatform() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMinimum() {
		return minimum;
	}

	public void setMinimum(String minimum) {
		this.minimum = minimum;
	}

	public String getDate() {
		return date;
	}
	
	public void setDate(String date) {
		this.date = date;
	}

	public void addDistribution(DownloadDistribution distribution) {
		distributions.add(distribution);
	}
	
	public List<DownloadDistribution> getDistributions() {
		return distributions;
	}
}
