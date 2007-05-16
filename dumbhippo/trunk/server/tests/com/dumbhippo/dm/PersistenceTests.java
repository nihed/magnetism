package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import com.dumbhippo.dm.persistence.TestGroup;

/**
 * The tests in this class check that the local in-memory EJB3 persistence
 * configuration we use for the other tests is working. 
 * 
 * @author otaylor
 */
public class PersistenceTests  extends TestCase {
	TestSupport support;
	
	public void testCommit() {
		TestGroup group;
		EntityManager em;
		String id;
		
		em = support.createEntityManager();
		
		em.getTransaction().begin();
		group = new TestGroup("Hippos");
		id = group.getId();
		em.persist(group);
		em.getTransaction().commit();
		
		//////////////////////////////////////
		
		em.getTransaction().begin();
		group = em.find(TestGroup.class, id);
		assertTrue(group != null);
		assertTrue(group.getName().equals("Hippos"));
		em.getTransaction().commit();
	}
	
	public void testRollback() {
		TestGroup group;
		EntityManager em;
		String id;
		
		em = support.createEntityManager();
		
		em.getTransaction().begin();
		group = new TestGroup("Hippos");
		id = group.getId();
		em.persist(group);
		em.getTransaction().rollback();
		
		//////////////////////////////////////
		
		em.getTransaction().begin();
		group = em.find(TestGroup.class, id);
		assertTrue(group == null);
		em.getTransaction().commit();
	}

	@Override
	protected void setUp()  {
		support = TestSupport.getInstance();
	}
	
	@Override
	protected void tearDown() {
		support = null;
	}
}
