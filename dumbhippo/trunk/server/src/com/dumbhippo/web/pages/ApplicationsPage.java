package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationView;
import com.dumbhippo.web.WebEJBUtil;

public class ApplicationsPage {
	private ApplicationSystem applicationSystem;
	public List<ApplicationView> popularApplications;
	private ApplicationCategory category;
	
	static final int ICON_SIZE = 48;

	public ApplicationsPage() {
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
	}
	
	public void setCategoryName(String categoryName) {
		for (ApplicationCategory c : ApplicationCategory.values()) {
			if (c.name().equalsIgnoreCase(categoryName)) {
				category = c;
				break;
			}
		}
	}
	
	public ApplicationCategory getCategory() {
		return category;
	}
	
	public List<ApplicationCategory> getCategories() {
		List<ApplicationCategory> results = new ArrayList<ApplicationCategory>();
		
		for (ApplicationCategory category : ApplicationCategory.values())
			results.add(category);
		
		Collections.sort(results, new Comparator<ApplicationCategory>() {
			public int compare(ApplicationCategory a, ApplicationCategory b) {
				// Sort OTHER after everything else
				if (a.equals(ApplicationCategory.OTHER))
					return b.equals(ApplicationCategory.OTHER) ? 0 : 1;
				else if (b.equals(ApplicationCategory.OTHER))
					return -1;
				else
					return a.getDisplayName().compareTo(b.getDisplayName());
			}
		});
		
		return results;
	}
	
	public List<ApplicationView> getPopularApplications() {
		if (popularApplications == null) {
			Date since = new Date(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);
			popularApplications = applicationSystem.getPopularApplications(since, ICON_SIZE, category);
		}
		
		return popularApplications;
	}
}
