package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class PollingTaskPersistenceBean implements PollingTaskPersistence {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PollingTaskPersistenceBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;


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

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public PollingTaskLoadResult loadNewTasks(final long dbId) {
		final Set<PollingTask> newTasks = new HashSet<PollingTask>();		
		final long[] largestId = {dbId};
		
		/* First get the list of ids in a single transaction */
		final List<Long> entries = new ArrayList<Long>();		
		runner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				String queryStr = "select task.id from PollingTaskEntry task";
				if (dbId >= 0)
					queryStr += " where task.id > :dbId";
				Query query = em.createQuery(queryStr);
				if (dbId >= 0)
					query.setParameter("dbId", dbId);
				entries.addAll(TypeUtils.castList(Long.class, query.getResultList()));
			}
		});

		/* Now load each task individually in a separate transaction since some of
		 * them may involve complex queries and we don't want to tie it to a single
		 * transaction.
		 */
		for (final Long id : entries) {
			runner.runTaskInNewTransaction(new Runnable() {
				public void run()  {
					PollingTaskEntry entry = em.find(PollingTaskEntry.class, id);
					if (entry == null) {
						logger.warn("Couldn't find entry with id=" + id);
						return;
					}
					if (entry.getId() > largestId[0])
						largestId[0] = entry.getId();
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
			});
		}
		return new PollingTaskLoadResultImpl(largestId[0], newTasks);
	}
}