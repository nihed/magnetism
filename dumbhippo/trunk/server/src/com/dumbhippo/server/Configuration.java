package com.dumbhippo.server;

import javax.ejb.Local;

/*
 * Represents the configuration of the Dumbhippo server; just
 * a quick hack, later we'll replace it with UniConf of course.
 * 
 * @author walters
 */
@Local
public interface Configuration {
	@SuppressWarnings("serial")
	public class PropertyNotFoundException extends Exception {
		public PropertyNotFoundException(String name) {
			super(name);
		}
	}
	public String getProperty(String name) throws PropertyNotFoundException;
	public String getProperty(String name, String defaultValue);
}
