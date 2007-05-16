package com.dumbhippo.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TestSupport {
	EntityManagerFactory emf;
	static TestSupport instance;
	
	public static synchronized TestSupport getInstance() {
		if (instance == null)
			instance = new TestSupport();
		
		return instance;
	}
	
	private TestSupport() {
		emf = Persistence.createEntityManagerFactory("dmtest");
	}
	
	public EntityManager createEntityManager() {
		return emf.createEntityManager();
	}
}
