package com.dumbhippo.server.impl;

import javax.ejb.Stateless;

import com.dumbhippo.server.ServerStatus;
import com.dumbhippo.statistics.ServerStatistics;

@Stateless
public class ServerStatusBean implements ServerStatus {
	
	public boolean isTooBusy() {
		ServerStatistics stats = new ServerStatistics();
		double inUse = stats.getDatabaseConnectionCount();
		double max = stats.getDatabaseMaxConnectionCount();
		
		return (inUse / max) > 0.9; 
	}	
}
