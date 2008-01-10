package com.dumbhippo.hungry.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
					" not set");
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
			String propfile = System.getenv("HUNGRY_PROPERTIES");
			InputStream str = null;
			if (propfile != null) {
				try {
					str = new FileInputStream(propfile);
				} catch (FileNotFoundException e) {
					System.err.println("Hungry properties file '" + propfile + "' not found.");
					System.exit(1);
				}
			} else {
				str = Config.class.getResourceAsStream("hungry.properties");
				if (str == null) {
					System.err.println("hungry.properties not found");
					System.exit(1);
				}
			}
			props.load(str);
			str.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Exception reading hungry.properties: " + e);
			System.exit(1);
		}
	}
}
