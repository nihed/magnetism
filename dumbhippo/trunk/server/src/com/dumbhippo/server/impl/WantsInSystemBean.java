package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ValidationException;
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
	
	public void addWantsIn(String untrustedAddress) throws ValidationException {
		
		if (untrustedAddress == null)
			throw new IllegalArgumentException("null wants in address");
		
		final String address = EmailResource.canonicalize(untrustedAddress);
		
		final boolean increment = true;
		
		try {
			runner.runTaskThrowingConstraintViolation(new Callable<WantsIn>() {

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
	
	public boolean isWantsIn(String emailAddress) {
		// this function is used when we are deciding whether the person who just joined
		// came from the WantsIn list, if we only want to return true in case the person
		// is in the wants in list and it was us who sent them an invitation, could add
		// " AND wi.invitationSent = TRUE"
	    Query q = em.createQuery("FROM WantsIn wi WHERE wi.address = :emailAddress");	    
		q.setParameter("emailAddress", emailAddress);

		try {
		    q.getSingleResult();
		    return true;
		} catch (EntityNotFoundException e) { 
			return false;
		} catch (NonUniqueResultException e) {
			// this should not be allowed by the database schema
			logger.error("Multiple entries match email address {} in WantsIn table", emailAddress);
			throw new RuntimeException("Multiple entries match email address " + emailAddress + " in WantsIn table");
		}
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


