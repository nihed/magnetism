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
	
	static Properties defaults;
	
	static {		
		defaults = new Properties();
		
		for (HippoProperty prop : HippoProperty.values()) {
			if (prop.getDefault() != null) {
				defaults.put(prop.getKey(), prop.getDefault());
			}
		}
	}
	
	Properties props;
	
	@PostConstruct
	public void init() {
		props = new Properties(defaults);
		try {
			InputStream str = ConfigurationBean.class.getResourceAsStream("dumbhippo.properties");
			props.load(str);
		} catch (IOException e) {
			logger.warn("Exception reading dumbhippo.properties", e);
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
}

