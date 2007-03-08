package com.dumbhippo.web.pages;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.applications.AppinfoFile;
import com.dumbhippo.server.applications.AppinfoUploadView;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationView;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class ApplicationPage extends AbstractSigninOptionalPage {
	private ApplicationSystem applicationSystem;
	private ApplicationView application;
	private AppinfoFile appinfoFile;
	private AppinfoUploadView upload;
	private Pageable<ApplicationView> relatedApplications;
	private List<AppinfoUploadView> uploadHistory;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	static final int ICON_SIZE = 48;
	static final int MINI_ICON_SIZE = 24;
	static final int APPLICATIONS_PER_PAGE = 5;
	
	public ApplicationPage() {
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
	}
		
	public void setApplicationId(String applicationId) {
		try {
			this.application = applicationSystem.getApplicationView(applicationId, ICON_SIZE);
		} catch (NotFoundException e) {
		}
	}
	
	public ApplicationView getApplication() {
		return application;
	}
	
	public void setUploadId(String uploadId) {
		try {
			upload = applicationSystem.getAppinfoUploadView(getViewpoint(), new Guid(uploadId));
		} catch (ParseException e) {
		} catch (NotFoundException e) {
		}
	}

	public AppinfoUploadView getUpload() {
		if (upload == null && application != null) {
			try {
				upload = applicationSystem.getCurrentUploadView(getViewpoint(), application.getApplication().getId());
			} catch (NotFoundException e) {
			}
		}
		
		return upload;
	}
	
	public AppinfoFile getAppinfoFile() {
		if (appinfoFile == null) {
			try {
				AppinfoUploadView upload = getUpload();
				if (upload != null)
					appinfoFile = applicationSystem.getAppinfoFile(upload.getUpload());
			} catch (NotFoundException e) {
				throw new RuntimeException("getAppinfoFile called for a non-existent application", e);
			}
		
			if (appinfoFile != null) {
				// We only want access to information that is read from the appinfoFile
				// on startup, so we can close it immediately.
				try {
					appinfoFile.close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing appinfo file", e);
				}
			}
		}
		
		return appinfoFile;
	}

	private Date getSince() {
		return new Date(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);
	}
		
	public Pageable<ApplicationView> getRelatedApplications() {
		if (relatedApplications == null) {
			relatedApplications = pagePositions.createPageable("relatedApplications", APPLICATIONS_PER_PAGE);
			relatedApplications.setSubsequentPerPage(APPLICATIONS_PER_PAGE);
			applicationSystem.pageRelatedApplications(application.getApplication(), getSince(), MINI_ICON_SIZE, null, relatedApplications);
		}
		
		return relatedApplications;
	}
	
	public List<AppinfoUploadView> getUploadHistory() {
		if (uploadHistory == null)
			uploadHistory = applicationSystem.getUploadHistory(getViewpoint(), application.getApplication());
		
		return uploadHistory;
	}
}
