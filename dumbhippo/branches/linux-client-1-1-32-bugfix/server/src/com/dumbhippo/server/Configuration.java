package com.dumbhippo.server;

import java.io.File;
import java.net.URL;

import javax.ejb.ApplicationException;
import javax.ejb.Local;

/**
 * Represents the configuration of the Dumbhippo server; just
 * a quick hack, later we'll replace it with UniConf of course.
 * 
 * @author walters
 */
@Local
public interface Configuration {

	static final public String HEADSHOTS_RELATIVE_PATH = "/headshots";
	static final public String GROUPSHOTS_RELATIVE_PATH = "/groupshots";
	static final public int SHOT_TINY_SIZE = 30;
	static final public int SHOT_SMALL_SIZE = 48;
	static final public int SHOT_MEDIUM_SIZE = 60;
	static final public int SHOT_LARGE_SIZE = 192;
	static final public String NOW_PLAYING_THEMES_RELATIVE_PATH = "/nowplaying-themes";
	static final public int NOW_PLAYING_THEME_WIDTH = 440;
	static final public int NOW_PLAYING_THEME_HEIGHT = 120;
	static final public String POSTINFO_RELATIVE_PATH = "/postinfo";
	
	@ApplicationException
	public class PropertyNotFoundException extends Exception {
		private static final long serialVersionUID = 0L;

		public PropertyNotFoundException(String name) {
			super(name);
		}
	}

	/**
	 * Looks up an arbitrarily-named Java property first in our config file 
	 * then in the system config file and finally in our hardcoded defaults
	 * from HippoProperty
	 *
	 * @param name name of the Java property
	 * @return the value
	 * @throws PropertyNotFoundException if the value would be null
	 */
	public String getProperty(String name) throws PropertyNotFoundException;
	
	/**
	 * Looks up one of the properties we know about in advance, but only 
	 * works for properties that have hardcoded fallback defaults since 
	 * those can never throw PropertyNotFoundException
	 * 
	 * @param name the property
	 * @return value of the property
	 */
	public String getProperty(HippoProperty name);
	
	/**
	 * Looks up one of the properties we know about in advance, 
	 * and allows you to ask for properties with no fallback emergency
	 * default because it can throw PropertyNotFoundException.
	 * You would use this method when the property is "optional"
	 * i.e. a legitimate installation of the server might not set it
	 * and PropertyNotFoundException is a normal occurrence you want
	 * to handle.
	 * 
	 * @param name the property
	 * @return value of the property
	 * @throws PropertyNotFoundException if property is not set
	 */
	public String getPropertyNoDefault(HippoProperty name) throws PropertyNotFoundException;
	
	/**
	 * Throws a RuntimeException if the property is not found, suitable for use 
	 * when the property is required for any correctly-configured server.
	 * 
	 * @param name the property
	 * @return value of the property
	 */
	public String getPropertyFatalIfUnset(HippoProperty name);
	
	/**
	 * Gets our base URL parsed into an URL object.
	 * @return the base URL for this server
	 */
	public URL getBaseUrl();
	
	/**
	 * Set a configuration property, use to override properties loaded 
	 * when the server starts up.
	 *  
	 * @param name property name
	 * @param value property value
	 */
	public void setProperty(String name, String value);
	
	public boolean isFeatureEnabled(String name);
	
	public File getWebRealPath();
}
