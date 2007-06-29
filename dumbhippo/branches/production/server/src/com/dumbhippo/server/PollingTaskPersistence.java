package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.polling.PollingTask;

@Local
public interface PollingTaskPersistence {
	public interface PollingTaskLoader {
		public void migrateTasks();
		public PollingTask loadTask(PollingTaskEntry entry) throws NotFoundException;
	}
	
	public void createTask(PollingTaskFamilyType family, String id);
	
	public void createTaskIdempotent(PollingTaskFamilyType family, String id);	
	
	public void snapshot(Set<PollingTask> task);
	
	public void clean(Set<PollingTask> obsolete);

	public interface PollingTaskLoadResult {
		public long getLastDbId();
		public Set<PollingTask> getTasks();
	}
	
	public PollingTaskLoadResult loadNewTasks(long dbId);
}
