package com.dumbhippo.web;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class DownloadBean extends BrowserBean {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DownloadBean.class);
	
	private Configuration configuration;
	
	private DownloadBean(HttpServletRequest request) {
		super(request);
		configuration = WebEJBUtil.defaultLookup(Configuration.class);	
	}
	
	public static DownloadBean getForRequest(HttpServletRequest request) {
		DownloadBean bean = (DownloadBean)request.getAttribute("download");
		if (bean == null) {
			bean = new DownloadBean(request);
			request.setAttribute("download", bean);
		}
		
		return bean;
	}
	
	public boolean getHaveDownload() {
		return getDownloadUrl() != null;
	}
	
	public String getDownloadUrl() {
		if (isFedora5Requested()) {
			return getDownloadUrlFedora5();
		} else if (isFedora6Requested()) {
			return getDownloadUrlFedora6();
		} else if (isWindowsRequested()) {
			return getDownloadUrlWindows();
		} else {
			return null;
		}
	}
	
	// if linuxRequested && haveDownload then this should always return non-null
	public String getDownloadUrlSrpm() {
		if (isFedora5Requested())
			return getDownloadUrlFedora5Srpm();
		else if (isFedora6Requested()) {
			return getDownloadUrlFedora6Srpm();
		} else {
			return null;
		}
	}
	
	public String getDownloadFor() {
		if (isFedora5Requested()) {
			return "Fedora Core 5";
		} else if (isFedora6Requested()) {
			return "Fedora Core 6";
		} else if (isWindowsRequested()) {
			return "Windows XP";
		} else {
			return null;
		}
	}
	
	public String getCurrentVersion() {
		if (isFedora5Requested() || isFedora6Requested()) {
			return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADINFO_LINUX_CURRENT);
		} else if (isWindowsRequested()) {
			return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADINFO_WINDOWS_CURRENT);
		} else {
			return null;
		}
	}
	
	public String getVersionDate() {
		if (isFedora5Requested() || isFedora5Requested()) {
			return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADINFO_LINUX_DATE);
		} else if (isWindowsRequested()) {
			return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADINFO_WINDOWS_DATE);
		} else {
			return null;
		}
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	public String getDownloadUrlFedora5() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_FEDORA5);
	}
	
	public String getDownloadUrlFedora6() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_FEDORA6);
	}
	
	public String getDownloadUrlFedora5Srpm() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_FEDORA5_SRPM);
	}
	
	public String getDownloadUrlFedora6Srpm() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_FEDORA6_SRPM);
	}
	
	public String getDownloadUrlLinuxTar() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX_TAR);
	}
	
	// deprecated
	public String getDownloadUrlLinux() {
		logger.warn("Some page is still referring to downloadUrlLinux instead of distribution-specific urls");
		return getDownloadUrlFedora5();
	}
	
	// deprecated
	public String getDownloadUrlLinuxSrpm() {
		logger.warn("Some page is still referring to downloadUrlLinuxSrpm instead of distribution-specific urls");
		return getDownloadUrlFedora5Srpm();
	}

}
