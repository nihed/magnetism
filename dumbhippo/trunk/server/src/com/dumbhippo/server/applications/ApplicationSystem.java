package com.dumbhippo.server.applications;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AppinfoUpload;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.persistence.ApplicationUserState;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Local
public interface ApplicationSystem {
	boolean canEditApplications(Viewpoint viewpoint);
		
	public List<String> getAllApplicationIds();	
	Application lookupById(String id) throws NotFoundException;
	
	void addUpload(Guid uploaderId, Guid uploadId, AppinfoFile appinfoFile, String comment);
	void deleteApplication(UserViewpoint viewpoint, String applicationId, String comment);
	void reinstallAllApplications();
	
	void revertApplication(UserViewpoint viewpoint, String applicationId, Guid uploadId, String comment) throws XmlMethodException;
	
	ApplicationIconView getIcon(String appId, int size) throws NotFoundException;
	ApplicationView getApplicationView(String appId, int iconSze) throws NotFoundException;
	AppinfoUploadView getCurrentUploadView(Viewpoint viewpoint, String appId) throws NotFoundException;
	AppinfoUpload getCurrentUpload(String appId) throws NotFoundException;
	AppinfoUploadView getAppinfoUploadView(Viewpoint viewpoint, Guid uploadId) throws NotFoundException;
	AppinfoFile getAppinfoFile(Guid uploadId) throws NotFoundException;
	AppinfoFile getAppinfoFile(AppinfoUpload upload) throws NotFoundException;
	
	void recordApplicationUsage(UserViewpoint viewpoint, Collection<ApplicationUsageProperties> usages);

	public List<AppinfoUploadView> getUploadHistory(Viewpoint viewpoint, Application application);
	public List<AppinfoUploadView> getUploadHistory(Viewpoint viewpoint, int maxItems);
	
	public void pagePopularApplications(Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable);
	public void pageRelatedApplications(Application relatedTo, Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable);
	public void pageMyApplications(UserViewpoint viewpoint, Date since, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable);
	public Date getMyApplicationUsageStart(UserViewpoint viewpoint);
	public List<String> getMyMostUsedApplicationIds(UserViewpoint viewpoint, Date since, int maxResults);
	
	public List<CategoryView> getPopularCategories(Date since);
	
	public List<Application> getApplicationsWithTitlePatterns();
	
	void pinApplicationIds(User user, List<String> applicationIds, boolean pin) throws RetryException;
	
	public List<ApplicationView> viewApplications(UserViewpoint viewpoint, List<Application> apps, int iconSize);
	
	public void writeAllApplicationsToXml(XmlBuilder xml, String distribution, String lang);	
	
	public List<Application> getPinnedApplications(User user); 
	public ApplicationUserState getUserState(User user, Application app) throws RetryException;

	void updateUsages();
	
	void search(String search, int iconSize, ApplicationCategory category, Pageable<ApplicationView> pageable);
}
