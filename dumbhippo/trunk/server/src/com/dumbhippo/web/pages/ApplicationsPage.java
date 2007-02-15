package com.dumbhippo.web.pages;

import java.util.Date;
import java.util.List;

import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.CategoryView;
import com.dumbhippo.web.WebEJBUtil;

public class ApplicationsPage {
	private ApplicationSystem applicationSystem;
	public List<CategoryView> popularApplications;

	public ApplicationsPage() {
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
	}
	
	public List<CategoryView> getPopularApplications() {
		if (popularApplications == null) {
			Date since = new Date(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);
			popularApplications = applicationSystem.getPopularApplications(since, 24);
		}
		
		return popularApplications;
	}
}
