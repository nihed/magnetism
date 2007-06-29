package com.dumbhippo.web;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.downloads.Download;
import com.dumbhippo.server.downloads.DownloadPlatform;

public class DownloadBean extends BrowserBean {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DownloadBean.class);
	
	private Configuration configuration;
	private Download download;
	private DownloadPlatform platform;
	
	private DownloadBean(HttpServletRequest request) {
		super(request);
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		
		String platformString = null;
		String distributionString = null; 
		String osVersion = null;
		String architecture = null;
		
		if (isLinuxRequested()) {
			platformString = "linux";
			if (getDistributionRequested() != null)
				distributionString = getDistributionRequested().toString();
			osVersion = getOsVersionRequested();
			architecture = getArchitectureRequested();
		} else if (isWindowsRequested()) {
			platformString = "windows";
		}

		if (platformString != null) {
			try {
				platform = configuration.getDownloads().findPlatform(platformString);
			} catch (NotFoundException e) {
			}
				
			try {
				download = configuration.getDownloads().findDownload(platformString, distributionString, osVersion, architecture);
			} catch (NotFoundException e) {
			}
		}
	}
	
	public static DownloadBean getForRequest(HttpServletRequest request) {
		DownloadBean bean = (DownloadBean)request.getAttribute("download");
		if (bean == null) {
			bean = new DownloadBean(request);
			request.setAttribute("download", bean);
		}
		
		return bean;
	}
	
	public DownloadPlatform getPlatform() {
		return platform;
	}
	
	public Download getDownload() {
		return download;
	}
}
