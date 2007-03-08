package com.dumbhippo.server.applications;

import com.dumbhippo.persistence.AppinfoUpload;
import com.dumbhippo.server.views.PersonView;

public class AppinfoUploadView {
	private AppinfoUpload upload;
	private PersonView uploader;
	private boolean current;
	
	public AppinfoUploadView(AppinfoUpload upload, PersonView uploader, boolean current) {
		this.upload = upload;
		this.uploader = uploader;
		this.current = current;
	}
	
	public AppinfoUpload getUpload() {
		return upload;
	}
	
	public PersonView getUploader() {
		return uploader;
	}
	
	public boolean isCurrent() {
		return current;
	}
}
