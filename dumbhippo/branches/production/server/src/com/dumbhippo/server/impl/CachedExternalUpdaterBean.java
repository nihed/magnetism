package com.dumbhippo.server.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

public abstract class CachedExternalUpdaterBean<Status> implements CachedExternalUpdater<Status> {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(CachedExternalUpdaterBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	protected EntityManager em; 	
	
	@EJB
	protected PollingTaskPersistence pollingPersistence;
	
	@EJB
	@IgnoreDependency
	protected Configuration configuration;	
	
	@EJB
	private TransactionRunner runner;
	
	protected abstract ExternalAccountType getAccountType();
	protected abstract PollingTaskFamilyType getTaskFamily();
	
	private String getName() {
		return getAccountType().getSiteName();
	}
	
	public Status getCachedStatus(User user) throws NotFoundException {
		ExternalAccount external = user.getAccount().getExternalAccount(getAccountType());
		if (external == null)
			throw new NotFoundException("User has no " + getName() + " external account: " + user);
		// sentiment and handle are checked in here
		return getCachedStatus(external);
	}
	
	public Status getCachedStatus(ExternalAccount external) throws NotFoundException {
		if (external.getAccountType() != getAccountType())
			throw new RuntimeException("Invalid account type " + external);
		
		if (external.getHandle() == null || external.getSentiment() != Sentiment.LOVE)
			throw new NotFoundException(getName() + " account has null handle or is unloved: " + external);
		return getCachedStatus(external.getHandle());
	}
	
	protected abstract Query getCachedStatusQuery(String handle);
	
	@SuppressWarnings("unchecked")
	public Status getCachedStatus(String handle) throws NotFoundException {
		Query q = getCachedStatusQuery(handle);
		
		try {
			return (Status) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("Have not cached a status for " + getName() + " handle: " + handle);
		}
	}
	
	public Set<String> getActiveUsers() {
		Query q = em.createQuery("SELECT ea.handle FROM ExternalAccount ea WHERE " + 
				" ea.accountType = " + getAccountType().ordinal() + 
				" AND ea.sentiment = " + Sentiment.LOVE.ordinal() + 
				" AND ea.handle IS NOT NULL " + 
				" AND ea.account.disabled = false AND ea.account.adminDisabled = false");
		// we need a set because multiple accounts can have the same external account
		Set<String> results = new HashSet<String>();
		results.addAll(TypeUtils.castList(String.class, q.getResultList()));
		return results;
	}
	
	public Collection<User> getAccountLovers(String handle) {
		String musicSharingCheck = "";
		if (getAccountType().isAffectedByMusicSharing()) {
			musicSharingCheck = " AND (ea.account.musicSharingEnabled = true ";
			if (AccountSystem.DEFAULT_ENABLE_MUSIC_SHARING)
				musicSharingCheck += " OR ea.account.musicSharingEnabled IS NULL";
			musicSharingCheck += ") ";
		}
		
		Query q = em.createQuery("SELECT ea.account.owner FROM ExternalAccount ea WHERE " +
				"  ea.accountType = " + getAccountType().ordinal() + 
				"  AND ea.sentiment = " + Sentiment.LOVE.ordinal() + 
				"  AND ea.handle = :handle " + 
				"  AND ea.account.disabled = false AND ea.account.adminDisabled = false" +
				musicSharingCheck);
		q.setParameter("handle", handle);
		return TypeUtils.castList(User.class, q.getResultList());
	}	
	
	protected abstract void doPeriodicUpdate(String handle);
	
	// avoid log messages in here that will happen on every call, or it could get noisy
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void periodicUpdate(String handle) {
		EJBUtil.assertNoTransaction();
		doPeriodicUpdate(handle);
	}
	
	protected abstract Class<? extends CachedExternalUpdater<Status>> getUpdater();
	
	protected void onExternalAccountChange(User user, ExternalAccount external) {
		if (!external.hasLovedAndEnabledType(getAccountType()))
			return;
		
		if (!configuration.isFeatureEnabled("pollingTask")) {
			final String username = external.getHandle();
			runner.runTaskOnTransactionCommit(new Runnable() {
				public void run() {
					EJBUtil.defaultLookup(getUpdater()).periodicUpdate(username);
				}
			});
		} else {
			pollingPersistence.createTaskIdempotent(getTaskFamily(), external.getHandle());			
		}			
	}
	
	public void migrateTasks() {
		for (String username : getActiveUsers()) {
			pollingPersistence.createTaskIdempotent(getTaskFamily(), username);
		}		
	}	
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		onExternalAccountChange(user, external);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		onExternalAccountChange(user, external);		
	}
	
	protected abstract PollingTask createPollingTask(String handle);
	
	public PollingTask loadTask(PollingTaskEntry entry) {
		String username = entry.getTaskId();
		return createPollingTask(username);
	}	
}
