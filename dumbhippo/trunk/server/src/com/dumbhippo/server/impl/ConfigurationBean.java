package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ejb.PostConstruct;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

/*
 * Implementation of Configuration
 * @author walters
 */
@Stateless
public class ConfigurationBean implements Configuration {
	
	static private final Log logger = GlobalSetup.getLog(ConfigurationBean.class);		
	
	Properties props;
	
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
}

