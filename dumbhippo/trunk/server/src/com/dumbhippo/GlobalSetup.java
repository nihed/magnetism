/**
 * 
 */
package com.dumbhippo;

import java.net.URL;

import org.slf4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.LoggerFactory;

/**
 * 
 * Try to avoid putting stuff in here...
 * 
 * @author hp
 * 
 */
public final class GlobalSetup {
	private volatile static boolean initialized = false;
	private volatile static boolean usesLog4j = true;

	private GlobalSetup() {
		// can't instantiate this thing
	}

	private static void initializeLogging() {
		URL url = GlobalSetup.class.getResource("log4j.properties");
		if (url == null)
			throw new RuntimeException("no log4j.properties found!");
		//System.out.println("log4j properties url: " + url.toString());
		PropertyConfigurator.configure(url);
		//BasicConfigurator.configure();
		
		//logger.info("Initialized logging, config = " + url);
	}
	
	private static void initialize() {
		if (initialized)
			return;

		if (usesLog4j)
			initializeLogging();
		
		initialized = true;
	}
	
	public static Logger getLogger(Class klass) {
		initialize();
		return LoggerFactory.getLogger(klass);
	}
	
	public static void disableLog4j() {
		if (initialized)
			throw new RuntimeException("Must disable log4j prior to using the logger");
		usesLog4j = false;
	}
}
