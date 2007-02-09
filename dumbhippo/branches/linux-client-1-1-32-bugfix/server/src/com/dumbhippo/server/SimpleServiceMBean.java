package com.dumbhippo.server;

import org.jboss.annotation.ejb.Management;

@Management
public interface SimpleServiceMBean {
	public void start() throws Exception;
	public void stop() throws Exception;
}
