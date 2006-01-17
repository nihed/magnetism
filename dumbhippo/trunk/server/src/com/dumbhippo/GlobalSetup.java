/**
 * 
 */
package com.dumbhippo;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
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

		initializeLogging();
		
		initialized = true;
	}
	
	public static Log getLog(Class klass) {
		initialize();
		return LogFactory.getLog(klass);
	}
	
	public static Logger getLogger(Class klass) {
		return LoggerFactory.getLogger(klass);
	}
}
