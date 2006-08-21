package com.dumbhippo.server.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

/*
 * Implementation of Configuration
 * @author walters
 */
@Stateless
public class ConfigurationBean implements Configuration {
	
	static private final Logger logger = GlobalSetup.getLogger(ConfigurationBean.class);		

	static private Properties overridenProperties = new Properties();
	
	private Properties props;
	
	private URL baseurl;
	
	@PostConstruct
	public void init() {
		logger.debug("Loading dumbhippo configuration properties");
		
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
	}

	public String getProperty(String name) throws PropertyNotFoundException {
		String ret = overridenProperties.getProperty(name);
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

	public URL getBaseUrl() {
		if (baseurl == null) {		
			String s = getPropertyFatalIfUnset(HippoProperty.BASEURL);
			try {
				baseurl = new URL(s);
			} catch (MalformedURLException e) {
				throw new RuntimeException("Server misconfiguration - base URL is invalid! '" + s + "'", e);
			}
		}
		return baseurl;
	}
	
	public void setProperty(String name, String value) {
		overridenProperties.put(name, value);
	}
}

