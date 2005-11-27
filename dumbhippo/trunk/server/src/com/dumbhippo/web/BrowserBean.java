package com.dumbhippo.web;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

public class BrowserBean implements Serializable {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(BrowserBean.class);

	private enum OS { Mac, Windows, Linux, Unknown };
	private enum Browser { Khtml, Gecko, Opera, IE, Unknown };
	
	private OS os;
	private Browser browser;
	private int browserVersion;
	
	private BrowserBean(HttpServletRequest request) {
		
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
		
		if (userAgent.contains("Gecko")) {
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
		} else if (userAgent.contains("Konqueror") ||
				userAgent.contains("Safari"))
			browser = Browser.Khtml;
		else if (userAgent.contains("Opera"))
			browser = Browser.Opera;
		else if (userAgent.contains("MSIE 5.0")) {
			browser = Browser.IE;
		} else if (userAgent.contains("MSIE 5.5")) {
			browser = Browser.IE;
			browserVersion = 55;
		} else if (userAgent.contains("MSIE 6") ||
				userAgent.contains("MSIE 7") ||
				userAgent.contains("MSIE 8")) {
			browser = Browser.IE;
			browserVersion = 60;
		}
		
		logger.debug("User agent is '" + userAgent + "' analysis " + this);
	}
	
	public static BrowserBean getForRequest(HttpServletRequest request) {
		// seems to be little point in caching a singleton
		return new BrowserBean(request);
	}

	public boolean isGecko() {
		return browser == Browser.Gecko;
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

	public boolean isIeAtLeast55() {
		return isIe() && browserVersion >= 55;
	}

	public boolean isIeAtLeast60() {
		return isIe() && browserVersion >= 60;
	}

	public boolean isKhtml() {
		return browser == Browser.Khtml;
	}

	public boolean isLinux() {
		return os == OS.Linux;
	}

	public boolean isMac() {
		return os == OS.Mac;
	}

	public boolean isOpera() {
		return browser == Browser.Opera;
	}

	public boolean isWindows() {
		return os == OS.Windows;
	}

	@Override
	public String toString() {
		return "{os=" + os + " browser=" + browser + " version=" + browserVersion + "}";
	}
}
