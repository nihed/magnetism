package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ejb.PostConstruct;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

/*
 * Implementation of Configuration
 * @author walters
 */
@Stateless
public class ConfigurationBean implements Configuration {
	
	static Log logger = LogFactory.getLog(ConfigurationBean.class);		
	
	Properties props;
	
	@PostConstruct
	public void init() {
		props = new Properties();
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

	public String getProperty(String name, String defaultValue) {
		try {
			return getProperty(name);
		} catch (PropertyNotFoundException e) {
			return defaultValue;
		}
	}
}

