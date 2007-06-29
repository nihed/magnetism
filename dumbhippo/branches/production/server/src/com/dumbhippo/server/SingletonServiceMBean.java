package com.dumbhippo.server;

import org.jboss.annotation.ejb.Management;

@Management
public interface SingletonServiceMBean extends SimpleServiceMBean {
	public void startSingleton();
	public void stopSingleton();
}
