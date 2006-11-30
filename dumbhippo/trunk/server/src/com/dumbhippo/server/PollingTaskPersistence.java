package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;

@Local
public interface PollingTaskPersistence {
	public void snapshot(Set<PollingTask> task);
	
	public void clean();

	public void initializeTasks(Set<PollingTask> tasks);
}
