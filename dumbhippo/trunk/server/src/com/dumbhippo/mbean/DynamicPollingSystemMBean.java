package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBean;

/** MBean that lives on one node in the cluster and handles polling
 */
public interface DynamicPollingSystemMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
