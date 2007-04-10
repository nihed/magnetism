/**
 * 
 */
package com.dumbhippo.polling;

public interface PollingTaskFamily {
	public long getDefaultPeriodicity();
	
	public long getMaxOutstanding();
	
	public long getMaxPerSecond();
	
	public String getName();
}