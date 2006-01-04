package com.dumbhippo.hungry.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
	private static Config def;

	private Properties props;
	
	public static Config getDefault() {
		if (def == null) {
			def = new Config();
		}
		return def;
	}
	
	public String getValue(ConfigValue value) {
		String v = props.getProperty(value.getProperty());
		if (v == null)
			throw new IllegalStateException("Improperly configured, key " + value.getProperty() +
					" not set (copy dhdeploy/files/conf/hungry.properties to src/test/java/com/dumbhippo/hungry/util/hungry.properties)");
		return v;
	}
	
	public int getIntValue(ConfigValue value) {
		String s = getValue(value);
		return Integer.parseInt(s);
	}
	
	private Config() {
		
		// FIXME this (and the same thing in server ConfigurationBean) 
		// may be backward, if system props are provided at runtime 
		// they should override the defaults not be the defaults
		
		props = new Properties(System.getProperties());
		try {
			InputStream str = Config.class.getResourceAsStream("hungry.properties");
			props.load(str);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Exception reading hungry.properties: " + e);
			System.exit(1);
		}
	}
}
