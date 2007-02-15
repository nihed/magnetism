package com.dumbhippo.server.applications;

import java.util.List;

import com.dumbhippo.persistence.ApplicationCategory;

public class CategoryView {
	private ApplicationCategory category;
	private List<ApplicationView> applications;
	
	public CategoryView(ApplicationCategory category, List<ApplicationView> applications) {
		this.category = category;
		this.applications = applications;
	}

	public List<ApplicationView> getApplications() {
		return applications;
	}

	public ApplicationCategory getCategory() {
		return category;
	}
}
