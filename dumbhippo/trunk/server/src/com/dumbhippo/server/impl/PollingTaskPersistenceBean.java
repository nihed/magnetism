package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class PollingTaskPersistenceBean implements PollingTaskPersistence {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PollingTaskPersistenceBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;


	public void createTask(int family, String id) {
		PollingTaskEntry entry = new PollingTaskEntry(PollingTaskFamilyType.values()[family], id);
		em.persist(entry);		
	}	
	
	private PollingTaskEntry getEntry(PollingTask task) {
		PollingTaskEntry entry;
		entry = (PollingTaskEntry) em.createQuery("select stats from PollingTaskEntry stats where family = :family and taskId = :id")
			.setParameter("family", PollingTaskFamilyType.valueOf(task.getFamily().getName()).ordinal())
			.setParameter("id", task.getIdentifier()).getSingleResult();
		return entry;
	}
	
	public void snapshot(Set<PollingTask> tasks) {
		for (PollingTask task : tasks) {
			synchronized (task) {
				if (task.isDirty()) {
					PollingTaskEntry stats = getEntry(task);
			
					Date lastExecuted = null;
					if (task.getLastExecuted() >= 0)
						lastExecuted = new Date(task.getLastExecuted());
					stats.setLastExecuted(lastExecuted);
			
					stats.setPeriodicityAverage(task.getPeriodicityAverage());
					task.flagClean();
				}
			}
		}
	}	
	
	public void clean(Set<PollingTask> tasks) {
		for (PollingTask task : tasks) {
			PollingTaskEntry stats = getEntry(task);
			em.remove(stats);
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
		long largestId = -1;
		for (PollingTaskEntry entry : entries) {
			if (entry.getId() > largestId)
				largestId = entry.getId();
			PollingTaskFamilyType taskFamilyType = entry.getFamily();
			PollingTaskLoader loader = EJBUtil.defaultLookup(taskFamilyType.getLoader());
			newTasks.addAll(loader.loadTasks(Collections.singleton(entry)));
		}
		return new PollingTaskLoadResultImpl(largestId, newTasks);
	}
}