package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class PollingTaskPersistenceBean implements PollingTaskPersistence {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PollingTaskPersistenceBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;


	public void createTask(PollingTaskFamilyType family, String id) {
		PollingTaskEntry entry = new PollingTaskEntry(family, id);
		em.persist(entry);		
	}	
	
	private PollingTaskEntry getEntry(PollingTaskFamilyType family, String id) {
		PollingTaskEntry entry;
		entry = (PollingTaskEntry) em.createQuery("select stats from PollingTaskEntry stats where family = :family and taskId = :id")
			.setParameter("family", family)
			.setParameter("id", id).getSingleResult();
		return entry;		
	}
	
	public void createTaskIdempotent(PollingTaskFamilyType family, String id) {
		try {
			getEntry(family, id);
		} catch (NoResultException e) {
			createTask(family, id);
		}
	}
	
	private PollingTaskEntry getEntry(PollingTask task) {
		return getEntry(PollingTaskFamilyType.valueOf(task.getFamily().getName()), task.getIdentifier());
	}
	
	public void snapshot(Set<PollingTask> tasks) {
		for (PollingTask task : tasks) {
			try {
			    task.syncStateToTaskEntry(getEntry(task));
			} catch (NoResultException e) {
				logger.warn("Task {} disappeared, will not sync its state to the database.", task);
			}
		}
	}	
	
	public void clean(Set<PollingTask> tasks) {
		for (PollingTask task : tasks) {
			try {
			    PollingTaskEntry stats = getEntry(task);
			    em.remove(stats);
			    logger.debug("Removed task {}", task);
			} catch (NoResultException e) {
				logger.warn("Tried to remove task {} that must have been already removed.", task);
			} 
		}
	}
	
	private static class PollingTaskLoadResultImpl implements PollingTaskLoadResult {
		private long lastDbId;
		private Set<PollingTask> tasks;
		
		public PollingTaskLoadResultImpl(long lastDbId, Set<PollingTask> tasks) {
			this.lastDbId = lastDbId;
			this.tasks = tasks;
		}

		public long getLastDbId() {
			return lastDbId;
		}

		public Set<PollingTask> getTasks() {
			return tasks;
		}
	}

	public PollingTaskLoadResult loadNewTasks(long dbId) {
		String queryStr = "select task from PollingTaskEntry task";
		if (dbId >= 0)
			queryStr += " where task.id > :dbId";
		Query query = em.createQuery(queryStr);
		if (dbId >= 0)
			query.setParameter("dbId", dbId);
		Set<PollingTask> newTasks = new HashSet<PollingTask>();
		List<PollingTaskEntry> entries = TypeUtils.castList(PollingTaskEntry.class, query.getResultList());
		long largestId = dbId;
		for (PollingTaskEntry entry : entries) {
			if (entry.getId() > largestId)
				largestId = entry.getId();
			PollingTaskFamilyType taskFamilyType = entry.getFamily();
			PollingTaskLoader loader = EJBUtil.defaultLookup(taskFamilyType.getLoader());
			try {
				PollingTask task = loader.loadTask(entry);
				task.syncStateFromTaskEntry(entry);
				newTasks.add(task);
			} catch (NotFoundException e) {
				logger.info("Couldn't create task for entry {}: {}", entry.getId(), e.getMessage());
			}
		}
		return new PollingTaskLoadResultImpl(largestId, newTasks);
	}
}