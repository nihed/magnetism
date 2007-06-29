package com.dumbhippo.server.applications;

import com.dumbhippo.persistence.ApplicationCategory;

public class CategoryView {
	private ApplicationCategory category;
	private int usageCount;

	public CategoryView(ApplicationCategory category) {
		this.category = category;
	}

	public int getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}

	public ApplicationCategory getCategory() {
		return category;
	}
}
