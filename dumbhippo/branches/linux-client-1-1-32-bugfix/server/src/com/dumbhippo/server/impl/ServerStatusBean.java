package com.dumbhippo.server.impl;

import javax.ejb.Stateless;

import com.dumbhippo.server.ServerStatus;
import com.dumbhippo.statistics.ServerStatistics;

@Stateless
public class ServerStatusBean implements ServerStatus {
	
	private double getActiveConnectionPercentage() {
		ServerStatistics stats = ServerStatistics.getInstance();
		double inUse = stats.getDatabaseConnectionCount();
		double max = stats.getDatabaseMaxConnectionCount();
		
		return (inUse / max);
	}
	
	public boolean isTooBusy() {
		return getActiveConnectionPercentage() > 0.95; 
	}	
	
	public boolean throttleXmppConnections() {
		return getActiveConnectionPercentage() > 0.6; 
	}		
}
