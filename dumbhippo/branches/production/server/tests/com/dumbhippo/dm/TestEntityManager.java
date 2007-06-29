package com.dumbhippo.dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

/**
 * This is a EntityManager for injecting into a EntityManager field of one of
 * our DMOs. It delegates to the current EntityManager in the test framework. It's
 * actually pretty pointless, since we could just inject the current entity
 * manager instead, but keeps things a little more like they are under JTA, where
 * the injected EntityManager is a delegate.
 *
 * @author otaylor
 */
public class TestEntityManager implements EntityManager {
	private TestEntityManagerFactory factory;
	
	public TestEntityManager(TestEntityManagerFactory factory) {
		this.factory = factory;
	}
	
	public EntityManager getDelegateEM() {
		return factory.getCurrentDelegate();
	}
	
	public void clear() {
		getDelegateEM().clear();
	}

	public void close() {
		getDelegateEM().close();
	}

	public boolean contains(Object o) {
		return getDelegateEM().contains(o);
	}

	public Query createNamedQuery(String name) {
		return getDelegateEM().createNamedQuery(name);
	}

	public Query createNativeQuery(String s) {
		return getDelegateEM().createNativeQuery(s);
	}

	public Query createNativeQuery(String s, Class c) {
		return getDelegateEM().createNativeQuery(s, c);
	}

	public Query createNativeQuery(String s1, String s2) {
		return getDelegateEM().createNativeQuery(s1, s2);
	}

	public Query createQuery(String s) {
		return getDelegateEM().createQuery(s);
	}

	public <T> T find(Class<T> c, Object o) {
		return getDelegateEM().find(c, o);
	}

	public void flush() {
		getDelegateEM().flush();
	}

	public Object getDelegate() {
		return getDelegateEM();
	}

	public FlushModeType getFlushMode() {
		return getDelegateEM().getFlushMode();
	}

	public <T> T getReference(Class<T> c, Object o) {
		return getDelegateEM().getReference(c, o);
	}

	public EntityTransaction getTransaction() {
		return getDelegateEM().getTransaction();
	}

	public boolean isOpen() {
		return getDelegateEM().isOpen();
	}

	public void joinTransaction() {
		getDelegateEM().joinTransaction();
	}

	public void lock(Object o, LockModeType lm) {
		getDelegateEM().lock(o, lm);
	}

	public <T> T merge(T o) {
		return getDelegateEM().merge(o);
	}

	public void persist(Object o) {
		getDelegateEM().persist(o);
	}

	public void refresh(Object o) {
		getDelegateEM().refresh(o);
	}

	public void remove(Object o) {
		getDelegateEM().remove(o);
	}

	public void setFlushMode(FlushModeType fm) {
		getDelegateEM().setFlushMode(fm);
	}
}
