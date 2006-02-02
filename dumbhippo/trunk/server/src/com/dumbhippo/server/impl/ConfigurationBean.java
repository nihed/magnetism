package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.ejb.PostConstruct;
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
	
	private Properties props;

	private URL baseurl;
	
	@PostConstruct
	public void init() {
		logger.debug("Loading dumbhippo configuration...");
		
		props = new Properties(System.getProperties());
		try {
			InputStream str = ConfigurationBean.class.getResourceAsStream("dumbhippo.properties");
			if (str != null)
				props.load(str);			
		} catch (IOException e) {
			logger.warn("Exception reading dumbhippo.properties", e);
		}
		// put in our hardcoded defaults if no other defaults were found
		for (HippoProperty prop : HippoProperty.values()) {
			//logger.debug("--system property was " + prop.getKey() + "=" + System.getProperties().getProperty(prop.getKey()));
			String loaded = props.getProperty(prop.getKey());
			
			// if we ever want an empty/whitespace string, we can add a flag to HippoProperty for 
			// whether it's allowed or something. But right now empty doesn't make sense for any of
			// our properties.
			if (loaded != null && loaded.trim().length() == 0) {
				logger.debug("Clearing empty property value for " + prop.getKey());
				props.setProperty(prop.getKey(), null);
			}
			
			if (prop.getDefault() != null && props.getProperty(prop.getKey()) == null) {
				//logger.debug("--loading hardcoded default " + prop.getKey() + "=" + prop.getDefault());
				props.put(prop.getKey(), prop.getDefault());
			} else {
				//logger.debug("--using property " + prop.getKey() + "=" + props.getProperty(prop.getKey()));
			}
		}
	}

	public String getProperty(String name) throws PropertyNotFoundException {
		String ret = props.getProperty(name);
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
}

