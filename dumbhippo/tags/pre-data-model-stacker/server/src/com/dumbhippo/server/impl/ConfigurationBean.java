package com.dumbhippo.server.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.dumbhippo.FuzzyStackTrace;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.downloads.DownloadConfiguration;
import com.dumbhippo.server.downloads.DownloadSaxHandler;
import com.dumbhippo.server.views.Viewpoint;

/*
 * Implementation of Configuration
 * @author walters
 */
@Service
public class ConfigurationBean implements Configuration, SimpleServiceMBean {
	
	static private final Logger logger = GlobalSetup.getLogger(ConfigurationBean.class);		

	static private Properties overriddenProperties = new Properties();
	
	static private Set<FuzzyStackTrace> loggedTraces = new HashSet<FuzzyStackTrace>();
	
	static synchronized void warnOncePerUniqueTrace(Throwable t) {
		FuzzyStackTrace fuzzy = new FuzzyStackTrace(t);
		if (loggedTraces.contains(fuzzy))
			return;
		loggedTraces.add(fuzzy);
		logger.warn("First instance of this stack trace, will not log it again", t);
	}
	
	private Properties props;
	private DownloadConfiguration downloads;
	
	private URL baseurlMugshot;

	private URL baseurlGnome;
	
	public void start() {
		Properties systemProperties = System.getProperties();
		
		// We don't want to use System.getProperties() as the default here, since 
		// then we couldn't substitute "" with null, as we do below
		props = new Properties();
		// we don't have a dumbhippo.properties for now, since there are no props set at build time
		/*
		try {
			InputStream str = ConfigurationBean.class.getResourceAsStream("dumbhippo.properties");
			if (str != null)
				props.load(str);
		} catch (IOException e) {
			logger.warn("Exception reading dumbhippo.properties", e);
		}
		*/
		
		// use values System.getProperties() or our hardcoded defaults
		for (HippoProperty prop : HippoProperty.values()) {
			// logger.debug("Finding value for property {}", prop.getKey());
			String loaded = props.getProperty(prop.getKey());
			// logger.debug("  dumbhippo.properties has value {}", loaded);
			if (loaded == null) {
				loaded = systemProperties.getProperty(prop.getKey());
				// logger.debug("  falling back to system value {}", loaded);
			}
			if (loaded == null) {
				loaded = prop.getDefault();
				// logger.debug("  falling back to default value {}", loaded);
			}
			
			// if we ever want an empty/whitespace string, we can add a flag to HippoProperty for 
			// whether it's allowed or something. But right now empty doesn't make sense for any of
			// our properties. We do this trimming after the fallbacks to allow unsetting
			// a property that was set in a lower-priority source.
			if (loaded != null && loaded.trim().length() == 0) {
				// logger.debug("  clearing empty property value");
				loaded = null;
			}
			
			if (loaded != null)
				props.put(prop.getKey(), loaded);
			else
				props.remove(prop.getKey());
		}
		
		String s = getPropertyFatalIfUnset(HippoProperty.BASEURL_MUGSHOT);
		try {
			baseurlMugshot = new URL(s);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Server misconfiguration - Mugshot base URL is invalid! '" + s + "'", e);
		}
		
		s = getPropertyFatalIfUnset(HippoProperty.BASEURL_GNOME);
		try {
			baseurlGnome = new URL(s);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Server misconfiguration - GNOME base URL is invalid! '" + s + "'", e);
		}
		
		String downloadXml = getPropertyFatalIfUnset(HippoProperty.DOWNLOADS);
		downloads = new DownloadConfiguration();
		
		try {
			DownloadSaxHandler handler = new DownloadSaxHandler(downloads);
			handler.parse(downloadXml);
		} catch (SAXParseException e) {
			throw new RuntimeException("Failed to parse downloads property: line " + e.getLineNumber() + ": " + e.getMessage());
		} catch (SAXException e) {
			throw new RuntimeException("Failed to parse downloads property", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse downloads property", e);
		}
	}
	
	public void stop() {
	}

	public String getProperty(String name) throws PropertyNotFoundException {
		String ret;
	
		synchronized(ConfigurationBean.class) {
			ret = overriddenProperties.getProperty(name);
		}
		if (ret == null)
		    ret = props.getProperty(name);
		if (ret == null)
			ret = System.getProperty(name);
		if (ret == null)
			throw new PropertyNotFoundException(name);
		return ret;
	}

	public String getProperty(HippoProperty name) {
		if (name.getDefault() == null) {
			throw new IllegalArgumentException("Need to use getPropertyNoDefault() for property " + name.getKey());
		}
		try {
			return getProperty(name.getKey());
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException("impossible! built-in property " + name.getKey() + " default vanished");
		}
	}

	public String getPropertyNoDefault(HippoProperty name) throws PropertyNotFoundException {
		return getProperty(name.getKey());
	}

	public String getPropertyFatalIfUnset(HippoProperty name) {
		try {
			return getPropertyNoDefault(name);
		} catch (PropertyNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public URL getBaseUrlMugshot() {
		return baseurlMugshot;
	}
	
	public URL getBaseUrlGnome() {
		return baseurlGnome;
	}
	

	public String getBaseUrl(Site site) {
		return getBaseUrlObject(site).toExternalForm();
	}

	public String getBaseUrl(Viewpoint viewpoint) {
		return getBaseUrl(viewpoint.getSite());
	}

	public URL getBaseUrlObject(Viewpoint viewpoint) {
		return getBaseUrlObject(viewpoint.getSite());
	}	
	
	public URL getBaseUrlObject(Site site) {
		if (site == Site.GNOME) {
			return baseurlGnome;
		} else if (site == Site.MUGSHOT) {
			return baseurlMugshot;
		} else {
			warnOncePerUniqueTrace(new Throwable("base URL requested from context where we don't know which one to use, site=" + site.name()));
			return baseurlMugshot;
		}
	}
	
	public void setProperty(String name, String value) {
		synchronized(ConfigurationBean.class) {
			overriddenProperties.put(name, value);
		}
	}

	public boolean isFeatureEnabled(String name) {
		return Arrays.asList(getProperty(HippoProperty.FEATURES).split(",")).contains(name);
	}
	
	static private File webRealPath;
	
	public File getWebRealPath() {
		synchronized(ConfigurationBean.class) {
			return webRealPath;
		}
	}
	
	public static synchronized void setWebRealPath(File file) {
		webRealPath = file;
		logger.debug("setting .war path to {}", webRealPath);
	}
	
	
	public DownloadConfiguration getDownloads() {
		return downloads;
	}

	public String getAdminJid(Site site) {
		if (site == Site.GNOME) {
			return getPropertyFatalIfUnset(HippoProperty.ADMIN_JID_GNOME);
		} else if (site == Site.MUGSHOT) {
			return getPropertyFatalIfUnset(HippoProperty.ADMIN_JID_MUGSHOT);
		} else {
			warnOncePerUniqueTrace(new Throwable("admin JID requested from context where we don't know which one to use, site=" + site.name()));
			return getPropertyFatalIfUnset(HippoProperty.ADMIN_JID_MUGSHOT);
		}
	}

	public String getAdminJid(Viewpoint viewpoint) {
		return getAdminJid(viewpoint.getSite());
	}
	
	public Site siteFromAdminJid(String adminJid) {
		if (getAdminJid(Site.GNOME).equals(adminJid))
			return Site.GNOME;
		else if (getAdminJid(Site.MUGSHOT).equals(adminJid))
			return Site.MUGSHOT;
		else {
			logger.warn("Can't map JID {} onto a site", adminJid);
			return Site.NONE;
		}
	}
}
