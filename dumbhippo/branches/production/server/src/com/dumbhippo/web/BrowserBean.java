package com.dumbhippo.web;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.util.EJBUtil;

public class BrowserBean implements Serializable {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(BrowserBean.class);

	private static final int LOWEST_GECKO_15_VERSION = 20051001;
	private static final int LOWEST_GECKO_10_VERSION = 20041101;
	
	private enum OS { Mac, Windows, Linux, Unknown };
	private enum Browser { Khtml, Gecko, Opera, IE, Unknown };
	public enum Distribution { Fedora, RHEL, Ubuntu, Unknown };
	
	private boolean forceIeAlphaImage = false;
	
	private OS os;
	private Browser browser;
	private boolean isFirefox;
	private Distribution distribution;
	private int browserVersion;
	
	private OS osRequested;
	private Browser browserRequested;
	private Distribution distributionRequested;
	private String osVersion;
	private String osVersionRequested;
	private String architecture;
	private String architectureRequested;
	
	protected BrowserBean(HttpServletRequest request) {
		
		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		forceIeAlphaImage = "true".equals(config.getProperty(HippoProperty.FORCE_IE_ALPHA_IMAGE));
		
		os = OS.Unknown;
		browser = Browser.Unknown;
		isFirefox = false;
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
		if (os == OS.Linux) {
			if (userAgent.contains("Fedora")) {
				if (userAgent.contains("fc5")) {
					distribution = Distribution.Fedora;
					osVersion = "5";
				} else if (userAgent.contains("fc6")) {
					distribution = Distribution.Fedora;
					osVersion = "6";
				} else if (userAgent.contains("fc7")) {
					distribution = Distribution.Fedora;
					osVersion = "7";
				}
			} else if (userAgent.contains("Red Hat")) {
				if (userAgent.contains("el4")) {
					distribution = Distribution.RHEL;
					osVersion = "4";
				} else if (userAgent.contains("el5")) {
					distribution = Distribution.RHEL;
					osVersion = "5";
				}
				
			} else if (userAgent.contains("Ubuntu")) {
				if (userAgent.contains("Ubuntu/dapper")) {
					distribution = Distribution.Ubuntu;
					osVersion = "6.05";
				} else if (userAgent.contains("Ubuntu-edgy")) {
					distribution = Distribution.Ubuntu;
					osVersion = "6.10";
				} else if (userAgent.contains("Ubuntu-feisty")) {
					distribution = Distribution.Ubuntu;
					osVersion = "7.04";
				}
			}
		
			// Order is important here - "i686 (x86_64)" is a 32-bit browser on a 64-bit
			// system, in which case we want to offer a 32-bit Mugshot download, so treat
			// as i686.
			
			if (userAgent.contains("i686") || userAgent.contains("i586") || userAgent.contains("i386"))
				architecture = "x86";
			else if (userAgent.contains("x86_64"))
				architecture = "x86_64";
			else if (userAgent.contains("ppc64"))
				architecture = "ppc64";
			else if (userAgent.contains("ppc"))
				architecture = "ppc";
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
					if (geckoVersion > LOWEST_GECKO_15_VERSION)
						browserVersion = 15;
					else if (geckoVersion > LOWEST_GECKO_10_VERSION)
						browserVersion = 10;
				} catch (NumberFormatException e) {
				}
			}
			if (userAgent.contains("Firefox")) 
				isFirefox = true;
		} else if (userAgent.contains("Opera")) {
			browser = Browser.Opera;
		} else if (os == OS.Windows && userAgent.contains("MSIE 5.0")) {
			browser = Browser.IE;
		} else if (os == OS.Windows && userAgent.contains("MSIE 5.5")) {
			browser = Browser.IE;
			browserVersion = 55;
		} else if (os == OS.Windows && userAgent.contains("MSIE 6")) {
			browser = Browser.IE;
			browserVersion = 60;
		} else if (os == OS.Windows && 
				(userAgent.contains("MSIE 7") ||
				 userAgent.contains("MSIE 8"))) {
			browser = Browser.IE;
			browserVersion = 70;
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
		
		osVersionRequested = request.getParameter("osVersion");
		architectureRequested = request.getParameter("architecture");
		
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
	
	// This method should not be used to determine html content for 
	// anonymous pages for which we use a single cache for all gecko browsers.
	public boolean isFirefox() {
		return isFirefox;
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
	
	public Distribution getDistribution() {
		return distribution; 
	}
	
	public Distribution getDistributionRequested() {
		return distributionRequested != null ? distributionRequested : distribution; 
	}
	
	public String getOsVersion() {
		return osVersion;
	}

	public String getOsVersionRequested() {
		return osVersionRequested != null ? osVersionRequested : osVersion;
	}

	public String getArchitecture() {
		return architecture;
	}
	
	public String getArchitectureRequested() {
		return architectureRequested != null ? architectureRequested : architecture;
	}
	
	public String getSupportedBrowsers() {
		return "Internet Explorer 6 and Firefox 1.5";
	}
	
	public boolean isSupported() {
		return isIeAtLeast60() || isGeckoAtLeast15();
	}
	
	public boolean getIeAlphaImage() {
		return isIeAtLeast55() && (forceIeAlphaImage || browserVersion < 70);
	}
	
	@Override
	public String toString() {
		return "{os=" + os + " " +
				"browser=" + browser + 
				" version=" + browserVersion + 
				" osRequested=" + osRequested + 
				" browserRequested=" + browserRequested + 
				" distribution=" + distribution + 
				" distributionRequested=" + distributionRequested +
				" osVersion=" + osVersion + 
				" osVersionRequested=" + osVersionRequested +
				" architecture=" + architecture + 
				" architectureRequested=" + architectureRequested + "}";
	}
	
	@SuppressWarnings("unused")
	private static void appendBool(StringBuilder sb, String jsVar, boolean value) {
		sb.append(jsVar);
		sb.append(" = ");
		sb.append(Boolean.toString(value));
		sb.append(";\n");
	}
	
	public String getJavascriptCode() {
		StringBuilder sb = new StringBuilder();
		
		// this is because the "dh" module doesn't exist in 
		// config.js
		sb.append("var tmp_dhBrowser = {};\n");
	
		// this approach means config.js can't be cached at the Apache level
		/*
		appendBool(sb, "tmp_dhBrowser.ie", isIe());
		appendBool(sb, "tmp_dhBrowser.ieAtLeast55", isIeAtLeast55());
		appendBool(sb, "tmp_dhBrowser.ieAtLeast60", isIeAtLeast60());
		appendBool(sb, "tmp_dhBrowser.gecko", isGecko());
		appendBool(sb, "tmp_dhBrowser.geckoAtLeast10", isGeckoAtLeast10());
		appendBool(sb, "tmp_dhBrowser.geckoAtLeast15", isGeckoAtLeast15());
		appendBool(sb, "tmp_dhBrowser.safari", isSafari());
		appendBool(sb, "tmp_dhBrowser.linux", isLinux());
		appendBool(sb, "tmp_dhBrowser.windows", isWindows());
		appendBool(sb, "tmp_dhBrowser.mac", isMac());
		appendBool(sb, "tmp_dhBrowser.fedora5", isFedora5());
		appendBool(sb, "tmp_dhBrowser.fedora6", isFedora6());
		*/
		
		// The reason for putting the js here is to help keep it in sync with 
		// the Java version of the same thing, though whether it's worth the 
		// pain of writing javascript in Java strings is debatable
		sb.append("var tmp_dhInitBrowser = function(browser) {\n");
		sb.append("    var gecko15 = ");
		sb.append(LOWEST_GECKO_15_VERSION);
		sb.append(";\n");
		sb.append("    var ua = navigator.userAgent;\n");
		sb.append("    var av = navigator.appVersion;\n");
		sb.append("    browser.khtml = av.indexOf('Safari') >= 0 || av.indexOf('Konqueror') >= 0;\n");
		sb.append("    var geckoPos = ua.indexOf('Gecko');\n");
		sb.append("    if (browser.khtml) geckoPos = -1;\n");
		sb.append("    browser.gecko = geckoPos >= 0;\n");
		sb.append("    if (geckoPos >= 0) {\n");
		sb.append("    	   var geckoVersion = parseInt(ua.substring(geckoPos + 6, geckoPos + 14));\n");
		sb.append("        browser.geckoAtLeast15 = geckoVersion >= gecko15;\n");
		sb.append("    } else {\n");
		sb.append("        browser.geckoAtLeast15 = false;\n");
		sb.append("    }\n");
		sb.append("    browser.ie = av.indexOf('MSIE ') >= 0;\n");
		sb.append("    browser.ieAtLeast55 = av.indexOf('MSIE 5.5') >= 0;\n");
		sb.append("    browser.ieAtLeast60 = av.indexOf('MSIE 6') >= 0;\n");
		sb.append("    browser.ieAtLeast70 = av.indexOf('MSIE 7') >= 0;\n");
		sb.append("    if (browser.ieAtLeast70) browser.ieAtLeast60 = true;\n");
		sb.append("    if (browser.ieAtLeast60) browser.ieAtLeast55 = true;\n");
		if (forceIeAlphaImage)
			sb.append("    browser.ieAlphaImage = browser.ieAtLeast55;\n");
		else
			sb.append("    browser.ieAlphaImage = browser.ieAtLeast55 && !browser.ieAtLeast70;\n");
		sb.append("    browser.linux = av.indexOf('X11') >= 0;\n");
		sb.append("    browser.windows = av.indexOf('Windows') >= 0;\n");
		sb.append("    browser.mac = av.indexOf('Macintosh') >= 0;\n");
		sb.append("}\n");
		sb.append("tmp_dhInitBrowser(tmp_dhBrowser);");
		sb.append("tmp_dhInitBrowser = null;");
		
		return sb.toString();
	}
}
