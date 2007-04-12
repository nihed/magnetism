package com.dumbhippo.polling;

import org.jboss.system.ServiceMBean;

import com.dumbhippo.persistence.PollingTaskFamilyType;

/** MBean that lives on one node in the cluster and handles polling
 */
public interface SwarmPollingSystemMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	public void executeTaskNow(PollingTaskFamilyType family, String id) throws Exception;	
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
