/**
 * 
 */
package com.dumbhippo.persistence;

import java.io.File;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.dumbhippo.persistence.Storage.SessionWrapper;
import com.dumbhippo.server.AuthenticationSystemBean;
import com.dumbhippo.server.IdentitySpiderBean;

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

	public static void initializeStorage() {
		SessionWrapper wrapper = Storage.getGlobalPerThreadSession();		
		wrapper.beginTransaction();
		// Initialize global singletons
		new IdentitySpiderBean().getTheMan();
		new AuthenticationSystemBean().getServerSecret();
		wrapper.commitTransaction();		
	}
	
	public static void initialize(File storageDir) {
		if (initialized)
			return;

		initializeLogging();
		logger.info("Booting");		
		
		Storage.initGlobalInstance(storageDir.toString());
		initializeStorage();
		
		logger.info("successfully initialized");
		
		initialized = true;
	}

	public static void initialize() {
		initialize(new File(System.getProperty("java.io.tmpdir"), "dumbhippo-storage"));
	}
}
