package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBean;

public interface FeedUpdaterMBean extends ServiceMBean {
	// Called when we become the cluster singleton
	void startSingleton();
	
	// Called when we are no longer the cluster singleton
	void stopSingleton();
}
