/**
 * 
 */
package com.dumbhippo.polling;

public interface PollingTaskFamily {
	public long getDefaultPeriodicitySeconds();
	
	public long rescheduleSeconds(long suggestedSeconds);
	
	public String getName();
}