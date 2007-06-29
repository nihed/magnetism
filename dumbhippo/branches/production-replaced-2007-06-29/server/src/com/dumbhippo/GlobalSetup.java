/**
 * 
 */
package com.dumbhippo;

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
	private GlobalSetup() {
		// can't instantiate this thing
	}
	
	public static Logger getLogger(Class klass) {
		return LoggerFactory.getLogger(klass);
	}
}
