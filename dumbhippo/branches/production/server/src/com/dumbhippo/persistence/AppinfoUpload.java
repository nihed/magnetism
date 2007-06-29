package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class AppinfoUpload extends EmbeddedGuidPersistable {
	private static final long serialVersionUID = 0L;
	
	private Application application;
	private User uploader;
	private long uploadDate;
	private boolean initialUpload;
	private boolean deleteApplication;
	private String comment;

	// For hibernate
	public AppinfoUpload() {
	}
	
	public AppinfoUpload(User uploader, String comment) {
		uploadDate = System.currentTimeMillis();
		this.uploader = uploader;
		this.comment = comment;
		this.deleteApplication = false;
	}
	
	@JoinColumn(nullable = false)
	@ManyToOne
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}

	@Column(nullable = false)
	public Date getUploadDate() {
		return new Date(uploadDate);
	}
	
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate.getTime();
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public User getUploader() {
		return uploader;
	}
	
	public void setUploader(User uploader) {
		this.uploader = uploader;
	}

	@Column(nullable = false)
	public boolean isDeleteApplication() {
		return deleteApplication;
	}

	public void setDeleteApplication(boolean delete) {
		this.deleteApplication = delete;
	}

	@Column(nullable = true)
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@Column(nullable = false)
	public boolean isInitialUpload() {
		return initialUpload;
	}

	public void setInitialUpload(boolean initialUpload) {
		this.initialUpload = initialUpload;
	}
}
