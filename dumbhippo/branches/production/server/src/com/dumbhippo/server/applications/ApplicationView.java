package com.dumbhippo.server.applications;

import com.dumbhippo.persistence.Application;

public class ApplicationView {
	private Application application;
	private int usageCount;
	private int rank;
	private ApplicationIconView icon;
	
	public ApplicationView(Application application) {
		this.application = application;
	}

	public Application getApplication() {
		return application;
	}

	public ApplicationIconView getIcon() {
		return icon;
	}

	public void setIcon(ApplicationIconView icon) {
		this.icon = icon;
	}

	public int getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
	
	public int getRank() {
		return rank;
	}
}
