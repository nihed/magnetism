package com.dumbhippo.server.applications;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface ApplicationSystem {
	void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile);
	void reinstallAllApplications();
	
	ApplicationIconView getIcon(String appId, int size) throws NotFoundException;
	ApplicationView getApplicationView(String appId, int iconSze) throws NotFoundException;
	
	void recordApplicationUsage(UserViewpoint viewpoint, Collection<ApplicationUsageProperties> usages);
	
	public void pagePopularApplications(Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable);
	public void pageRelatedApplications(Application relatedTo, Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable);
	
	public List<CategoryView> getPopularCategories(Date since);
	
	public List<Application> getApplicationsWithTitlePatterns();
}
