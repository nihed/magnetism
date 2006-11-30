package com.dumbhippo.server.impl;

import java.util.Date;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.persistence.CachedPollingTaskStats;
import com.dumbhippo.server.PollingTaskPersistence;

@Stateless
public class PollingTaskPersistenceBean implements PollingTaskPersistence {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PollingTaskPersistenceBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	private CachedPollingTaskStats getStats(PollingTask task) {
		CachedPollingTaskStats stats;
		try {
			stats = (CachedPollingTaskStats) em.createQuery("select stats from CachedPollingTaskStats stats where family = :family and id = :id")
			.setParameter("family", task.getFamily().getName())
			.setParameter("id", task.getIdentifier()).getSingleResult();
		} catch (NoResultException e) {
			stats = new CachedPollingTaskStats(task.getFamily().getName(), task.getIdentifier());
			em.persist(stats);
		}
		return stats;
	}
	
	public void snapshot(Set<PollingTask> tasks) {
		for (PollingTask task : tasks) {
			CachedPollingTaskStats stats = getStats(task);
			
			Date lastExecuted = null;
			if (task.getLastExecuted() >= 0)
				lastExecuted = new Date(task.getLastExecuted());
			stats.setLastExecuted(lastExecuted);
			
			stats.setPeriodicityAverage(task.getPeriodicityAverage());
		}
	}	
	
	public void initializeTasks(Set<PollingTask> tasks) {
		for (PollingTask task : tasks) {
			CachedPollingTaskStats stats = getStats(task);
			
			// Don't set last executed, the system doesn't care about it
			task.setPeriodicityAverage(stats.getPeriodicityAverage());
		}
	}	
	
	public void clean() {
		// TODO: noop for now...later we should clean out tasks which haven't been
		// executed in a week or something
	}
}