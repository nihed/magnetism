package com.dumbhippo.mbean;

public interface PeriodicJob {
	/** How often to run the job */
	public long getFrequencyInMilliseconds();
	
	/** Called every period to do the work. Should throw InterruptedException (which will make the 
	 * periodic job thread shut down cleanly) if it's interrupted.
	 */
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException;
	
	/** Name used in log messages */
	public String getName();
}
