package com.dumbhippo.server.impl;

import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.WantsIn;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class WantsInSystemBean implements WantsInSystem {

	static private final Log logger = GlobalSetup.getLog(WantsInSystem.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;	
	
	public void addWantsIn(String address) {
		
		if (address == null)
			throw new IllegalArgumentException("null wants in address");
		
		WantsInSystem proxy = (WantsInSystem) ejbContext.lookup(WantsInSystem.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				WantsIn wantsIn = proxy.findOrCreateWantsIn(address, true);
				logger.debug("'" + address + "' wants in " + wantsIn.getCount() + " times");
				return;
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating WantsIn, retrying", e);
					retries--;
				} else {
					logger.error("Couldn't create WantsIn");
					logger.trace(e);						
					throw new RuntimeException("Unexpected error creating WantsIn", e);
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public WantsIn findOrCreateWantsIn(String address, boolean increment) {
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
}
