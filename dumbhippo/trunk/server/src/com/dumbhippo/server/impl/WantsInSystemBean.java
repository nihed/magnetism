package com.dumbhippo.server.impl;

import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.WantsInSystem;

@Stateless
public class WantsInSystemBean implements WantsInSystem {

	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(WantsInSystem.class);
	
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
}
