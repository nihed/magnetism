package com.dumbhippo.web;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class BrowserBean implements Serializable {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(BrowserBean.class);

	private enum OS { Mac, Windows, Linux, Unknown };
	private enum Browser { Khtml, Gecko, Opera, IE, Unknown };
	private enum Distribution { Fedora5, Fedora6, Unknown };
	
	private OS os;
	private Browser browser;
	private Distribution distribution;
	private int browserVersion;
	
	private OS osRequested;
	private Browser browserRequested;
	private Distribution distributionRequested;
	
	protected BrowserBean(HttpServletRequest request) {
		
		os = OS.Unknown;
		browser = Browser.Unknown;
		browserVersion = 0;
				
		String userAgent = request.getHeader("User-Agent");
		if (userAgent == null) {
			logger.debug("No User-Agent");
			return;
		}
		
		if (userAgent.contains("X11"))
			os = OS.Linux;
		else if (userAgent.contains("Macintosh"))
			os = OS.Mac;
		else if (userAgent.contains("Windows"))
			os = OS.Windows;

		distribution = Distribution.Unknown;
		if (userAgent.contains("Fedora")) {
			if (userAgent.contains("fc5"))
				distribution = Distribution.Fedora5;
			else if (userAgent.contains("fc6"))
				distribution = Distribution.Fedora6;
		}
		
		/* note that we aren't counting IE before 5.0 or not on Windows 
		 * as IE ... it's just Unknown.
		 * For Netscape 4 and old Mozilla I'm guessing we should do
		 * the same but would require some research into
		 * gecko/netscape user agent history to see which changes make
		 * sense. 
		 */
		
		if (userAgent.contains("Konqueror") ||
		    userAgent.contains("Safari"))
		    browser = Browser.Khtml;
		else if (userAgent.contains("Gecko")) {
			browser = Browser.Gecko;
			Pattern p = Pattern.compile("Gecko\\/([0-9]+)");
			Matcher m = p.matcher(userAgent);
			if (m.find()) {
				try {
					int geckoVersion = Integer.parseInt(m.group(1));
					if (geckoVersion > 20051001)
						browserVersion = 15;
					else if (geckoVersion > 20041101)
						browserVersion = 10;
				} catch (NumberFormatException e) {
				}
			}
		} else if (userAgent.contains("Opera")) {
			browser = Browser.Opera;
		} else if (os == OS.Windows && userAgent.contains("MSIE 5.0")) {
			browser = Browser.IE;
		} else if (os == OS.Windows && userAgent.contains("MSIE 5.5")) {
			browser = Browser.IE;
			browserVersion = 55;
		} else if (os == OS.Windows && 
				(userAgent.contains("MSIE 6") ||
				 userAgent.contains("MSIE 7") ||
				 userAgent.contains("MSIE 8"))) {
			browser = Browser.IE;
			browserVersion = 60;
		}
		
		String platformOverrideStr = request.getParameter("platform");
		String browserOverrideStr = request.getParameter("browser");
		String distributionOverrideStr = request.getParameter("distribution");
		osRequested = null;
		browserRequested = null;
		distributionRequested = null;
		
		if (platformOverrideStr != null) {
			platformOverrideStr = platformOverrideStr.toLowerCase();
			for (OS o : OS.values()) {
				if (o.name().toLowerCase().equals(platformOverrideStr)) {
					osRequested = o;
					break;
				}
			}
		}

		if (browserOverrideStr != null) {
			browserOverrideStr = browserOverrideStr.toLowerCase();
			for (Browser b : Browser.values()) {
				if (b.name().toLowerCase().equals(browserOverrideStr)) {
					browserRequested = b;
					break;
				}
			}
			/* allow special browser=safari parameter */
			if (browserRequested == null && browserOverrideStr.equals("safari")) {
				osRequested = OS.Mac;
				browserRequested = Browser.Khtml;
			}
		}
		
		if (distributionOverrideStr != null) {
			distributionOverrideStr = distributionOverrideStr.toLowerCase();
			for (Distribution d : Distribution.values()) {
				if (d.name().toLowerCase().equals(distributionOverrideStr)) {
					distributionRequested = d;
					// implies platform=linux
					if (osRequested == null)
						osRequested = OS.Linux;
					break;
				}
			}
		}
		
		logger.debug("User agent is '{}' analysis {}", userAgent, this);
	}
	
	public static BrowserBean getForRequest(HttpServletRequest request) {
		BrowserBean bean = (BrowserBean)request.getAttribute("browser");
		if (bean == null) {
			bean = new BrowserBean(request);
			request.setAttribute("browser", bean);
		}
		
		return bean;
	}

	/* 
	 * The "isFooRequested" flavors indicate whether the user explicitly 
	 * chose the given platform/browser, e.g. on /bookmark where you can
	 * look at other browser versions even if you're using a different browser.
	 * 
	 * The plain "isFoo" indicate what user agent someone is using, 
	 * which should be used if you're trying to choose which html/css to 
	 * display or something like that. 
	 */
	
	private boolean isRequested(Browser b) {
		return browserRequested == b || (browserRequested == null && browser == b);
	}
	
	private boolean isRequested(OS o) {
		return osRequested == o || (osRequested == null && os == o);
	}
	
	private boolean isRequested(Distribution d) {
		return distributionRequested == d || (distributionRequested == null && distribution == d);
	}
	
	public boolean isGecko() {
		return browser == Browser.Gecko;
	}

	public boolean isGeckoRequested() {
		return isRequested(Browser.Gecko);
	}
	
	public boolean isGeckoAtLeast10() {
		return isGecko() && browserVersion >= 10;
	}

	public boolean isGeckoAtLeast15() {
		return isGecko() && browserVersion >= 15;
	}

	public boolean isIe() {
		return browser == Browser.IE;
	}

	public boolean isIeRequested() {
		return isRequested(Browser.IE);
	}
	
	public boolean isIeAtLeast55() {
		return isIe() && browserVersion >= 55;
	}

	public boolean isIeAtLeast60() {
		return isIe() && browserVersion >= 60;
	}

	public boolean isKhtml() {
		return browser == Browser.Khtml;
	}

	public boolean isKhtmlRequested() {
		return isRequested(Browser.Khtml);
	}
	
	public boolean isLinux() {
		return os == OS.Linux;
	}

	public boolean isLinuxRequested() {
		return isRequested(OS.Linux);
	}
	
	public boolean isMac() {
		return os == OS.Mac;
	}

	public boolean isMacRequested() {
		return isRequested(OS.Mac);
	}
	
	public boolean isOpera() {
		return browser == Browser.Opera;
	}

	public boolean isOperaRequested() {
		return isRequested(Browser.Opera);
	}
	
	public boolean isWindows() {
		return os == OS.Windows;
	}
	
	public boolean isWindowsRequested() {
		return isRequested(OS.Windows);
	}

	public boolean isSafari() {
		return isMac() && isKhtml();
	}

	public boolean isSafariRequested() {
		return isMacRequested() && isKhtmlRequested();
	}
	
	public boolean isFedora5() {
		return distribution == Distribution.Fedora5;
	}
	
	public boolean isFedora6() {
		return distribution == Distribution.Fedora6;
	}
	
	public boolean isFedora5Requested() {
		return isRequested(Distribution.Fedora5);
	}
	
	public boolean isFedora6Requested() {
		return isRequested(Distribution.Fedora6);
	}
	
	public String getSupportedBrowsers() {
		return "Internet Explorer 6 and Firefox 1.5";
	}
	
	public boolean isSupported() {
		return isIeAtLeast60() || isGeckoAtLeast15();
	}
	
	public boolean getIeAlphaImage() {
		return isIeAtLeast55() && browserVersion < 70;
	}
	
	@Override
	public String toString() {
		return "{os=" + os + " browser=" + browser + " version=" + browserVersion + " osRequested=" + osRequested + " browserRequested=" + browserRequested + " distribution=" + distribution + " distributionRequested=" + distributionRequested + "}";
	}
}
