package com.dumbhippo.server;

import javax.ejb.ApplicationException;
import javax.ejb.Local;

/*
 * Represents the configuration of the Dumbhippo server; just
 * a quick hack, later we'll replace it with UniConf of course.
 * 
 * @author walters
 */
@Local
public interface Configuration {

	@ApplicationException
	public class PropertyNotFoundException extends Exception {
		private static final long serialVersionUID = 0L;

		public PropertyNotFoundException(String name) {
			super(name);
		}
	}

	public String getProperty(String name) throws PropertyNotFoundException;
	public String getProperty(HippoProperty name);
	public String getPropertyNoDefault(HippoProperty name) throws PropertyNotFoundException;
}
