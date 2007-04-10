package com.dumbhippo.polling;

import org.jboss.system.ServiceMBean;

/** MBean that lives on one node in the cluster and handles polling
 */
public interface DynamicPollingSystemMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	public void pokeTaskSet(int index);	
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
