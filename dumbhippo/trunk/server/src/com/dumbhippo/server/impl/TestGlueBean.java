/**
 * 
 */
package com.dumbhippo.server.impl;

import javax.annotation.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TestGlueRemote;

/**
 * @author hp
 *
 */
public class TestGlueBean implements TestGlueRemote {

	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	private transient IdentitySpider identitySpider;
	
	/** 
	 * Used by the app server to provide us with an IdentitySpider
	 * @param identitySpider the spider
	 */
	@EJB
	protected void setIdentitySpider(IdentitySpider identitySpider) {
		this.identitySpider = identitySpider;
	}
	
	/* (non-Javadoc)
	 * @see com.dumbhippo.server.TestGlueRemote#loadTestData()
	 */
	public void loadTestData() {
		
		
		
	}

}
