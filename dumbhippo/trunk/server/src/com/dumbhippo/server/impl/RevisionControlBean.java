package com.dumbhippo.server.impl;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Revision;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.RevisionControl;

@Stateless
public class RevisionControlBean implements RevisionControl {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(RevisionControlBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private Notifier notifier;	
	
	public void persistRevision(Revision revision) {
		logger.debug("Saving new revision {}", revision);
		em.persist(revision);
		notifier.onRevisionAdded(revision);
	}

}
