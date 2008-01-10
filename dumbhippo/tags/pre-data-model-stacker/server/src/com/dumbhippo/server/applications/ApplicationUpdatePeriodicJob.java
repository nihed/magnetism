package com.dumbhippo.server.applications;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.mbean.PeriodicJob;
import com.dumbhippo.server.util.EJBUtil;

public class ApplicationUpdatePeriodicJob implements PeriodicJob {
	static private final Logger logger = GlobalSetup.getLogger(ApplicationUpdatePeriodicJob.class);
	
	static final long APPLICATION_UPDATE_TIME = 1000 * 60 * 29; // 29 minutes
	
	public long getFrequencyInMilliseconds() {
		return APPLICATION_UPDATE_TIME;
	}
	
	public void doIt(long sleepTime, int generation, int iteration) throws InterruptedException {
		ApplicationSystem applicationSystem = EJBUtil.defaultLookup(ApplicationSystem.class);
		
		logger.debug("Starting periodic application update task");
		applicationSystem.updateUsages();
		logger.debug("Finished periodic application update task");
	}

	public String getName() {
		return "application update";
	}	
}
