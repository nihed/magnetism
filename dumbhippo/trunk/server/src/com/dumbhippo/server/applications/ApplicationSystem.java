package com.dumbhippo.server.applications;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface ApplicationSystem {
	void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile);
	
	ApplicationIconView getIcon(String appId, int size) throws NotFoundException;
	
	void recordApplicationUsage(UserViewpoint viewpoint, Collection<ApplicationUsageProperties> usages);
	
	public List<ApplicationView> getPopularApplications(Date since, int iconSize, ApplicationCategory category);
}
