package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBean;

/** MBean that lives on one node in the cluster and handles "cron jobs"
 */
public interface PeriodicJobRunnerMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
