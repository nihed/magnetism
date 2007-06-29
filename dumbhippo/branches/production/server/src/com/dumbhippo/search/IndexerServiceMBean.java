package com.dumbhippo.search;

import org.jboss.system.ServiceMBean;

public interface IndexerServiceMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
