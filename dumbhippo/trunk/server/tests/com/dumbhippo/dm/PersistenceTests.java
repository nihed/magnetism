package com.dumbhippo.dm;

import javax.persistence.EntityManager;

import com.dumbhippo.dm.persistence.TestGroup;

/**
 * The tests in this class check that the local in-memory EJB3 persistence
 * configuration we use for the other tests is working. 
 * 
 * @author otaylor
 */
public class PersistenceTests  extends AbstractSupportedTests {
	public void testCommit() {
		TestGroup group;
		EntityManager em;
		String id;
		
		em = support.beginTransaction();
		group = new TestGroup("Hippos");
		id = group.getId();
		em.persist(group);
		em.getTransaction().commit();
		
		//////////////////////////////////////
		
		em = support.beginTransaction();
		group = em.find(TestGroup.class, id);
		assertTrue(group != null);
		assertTrue(group.getName().equals("Hippos"));
		em.getTransaction().commit();
	}
	
	public void testRollback() {
		TestGroup group;
		EntityManager em;
		String id;
		
		em = support.beginTransaction();
		group = new TestGroup("Hippos");
		id = group.getId();
		em.persist(group);
		em.getTransaction().rollback();
		
		//////////////////////////////////////
		
		em = support.beginTransaction();
		group = em.find(TestGroup.class, id);
		assertTrue(group == null);
		em.getTransaction().commit();
	}
}
