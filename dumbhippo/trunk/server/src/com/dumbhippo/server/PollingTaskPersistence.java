package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.persistence.PollingTaskEntry;

@Local
public interface PollingTaskPersistence {
	public interface PollingTaskLoader {
		public void migrateTasks();
		public Set<PollingTask> loadTasks(Set<PollingTaskEntry> entries);
	}
	
	public void createTask(int i, String id);
	
	public void snapshot(Set<PollingTask> task);
	
	public void clean(Set<PollingTask> obsolete);

	public interface PollingTaskLoadResult {
		public long getLastDbId();
		public Set<PollingTask> getTasks();
	}
	
	public PollingTaskLoadResult loadNewTasks(long dbId);
}
