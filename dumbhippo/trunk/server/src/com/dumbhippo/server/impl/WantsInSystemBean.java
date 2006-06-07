package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.WantsInView;

@Stateless
public class WantsInSystemBean implements WantsInSystem {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(WantsInSystem.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	public void addWantsIn(final String address) {
		
		if (address == null)
			throw new IllegalArgumentException("null wants in address");
		
		final boolean increment = true;
		
		try {
			runner.runTaskRetryingOnConstraintViolation(new Callable<WantsIn>() {

				public WantsIn call() {
					Query q;
					
					q = em.createQuery("FROM WantsIn w WHERE w.address = :address");
					q.setParameter("address", address);
					
					WantsIn wantsIn;
					try {
						wantsIn = (WantsIn) q.getSingleResult();
						if (increment) {
							wantsIn.incrementCount();
							em.persist(wantsIn);
						}
					} catch (EntityNotFoundException e) {
						wantsIn = new WantsIn(address);
						if (increment)
							wantsIn.incrementCount();
						em.persist(wantsIn);
					}
					
					return wantsIn;
				}			
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return; // not reached
		}
	}
	
	public List<WantsIn> getWantsInWithoutInvites(int count) {
		Query q = em.createQuery("FROM WantsIn wi " +
				                 "WHERE wi.invitationSent = FALSE " +
				                 "AND wi.address NOT IN  " +
				                 "(SELECT er.email from EmailResource er " +
				                 "WHERE (er IN (select it.invitee from InvitationToken it) OR " +
				                 "er IN (select ac.resource from AccountClaim ac)))" +
				                 "ORDER BY wi.creationDate ASC");

		q.setMaxResults(count);
			
		List<?> objects = q.getResultList();
		List<WantsIn> results = new ArrayList<WantsIn>(); 
		for (Object o : objects) {
			assert o != null;
			results.add((WantsIn)o);
		}
		
		return results;
	}
	
	public List<WantsInView> getWantsInViewsWithoutInvites(int count) {
		
		List<WantsIn> wantsInList = getWantsInWithoutInvites(count);

		List<WantsInView> results = new ArrayList<WantsInView>(); 
		for (WantsIn wi : wantsInList) {
			results.add(new WantsInView(wi.getAddress(), wi.getCount(), wi.getCreationDate()));
		}
		
		return results;
	}
	
	public int getWantsInCount() {
		Query q = em.createQuery("SELECT COUNT(wi) FROM WantsIn wi " +
				                 "WHERE wi.invitationSent = FALSE AND wi.address NOT IN  " +
				                 "(SELECT er.email from EmailResource er " +
				                 "WHERE (er IN (select it.invitee from InvitationToken it) OR " +
				                 "er IN (select ac.resource from AccountClaim ac)))");		
		Number count = (Number) q.getSingleResult();
		return count.intValue();		
	}
}


