/**
 * 
 */
package com.dumbhippo.server;

import java.net.URL;

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
final class GlobalSetup {
	private static boolean initialized = false;

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

	public static void initialize() {
		if (initialized)
			throw new Error("Can only do global initialization one time");

		initializeLogging();

		initialized = true;
	}
}
