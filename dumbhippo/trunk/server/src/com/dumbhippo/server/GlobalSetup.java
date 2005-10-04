/**
 * 
 */
package com.dumbhippo.server;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

/**
 * 
 * Try to avoid putting stuff in here...
 * 
 * There's no point making this thread safe because trying it from multiple
 * threads doesn't make any sense (you can only call it once!)
 * 
 * @author hp
 * 
 */
public final class GlobalSetup {
	private static boolean initialized = false;
	
	static Log logger = LogFactory.getLog(GlobalSetup.class);	

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
	}
	
	public static void initializeBase() {
		if (initialized)
			return;		
		initializeLogging();
	}
	
	public static void initialize() {
		if (initialized)
			return;

		initializeLogging();
		logger.info("Booting");		
		
		logger.info("successfully initialized");
		
		initialized = true;
	}
}
