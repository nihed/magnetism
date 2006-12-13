/*
 * This is a cut-and-paste of the TreeCache glue object from Hibernate
 * with a workaround for the JBossCache bug JBCACHE-785 and a change
 * to use a more "bushy" structure for the cache
 * (see /http://bugzilla.dumbhippo.com/show_bug.cgi?id=895)
 */

//$Id: TreeCache.java 9965 2006-05-30 18:00:28Z steve.ebersole@jboss.com $
package com.dumbhippo.persistence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheKey;
import org.jboss.cache.Fqn;
import org.jboss.cache.lock.TimeoutException;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * Represents a particular region within the given JBossCache TreeCache.
 *
 * @author Gavin King
 */
public class TreeCache implements Cache {
	
	static private final Logger log = GlobalSetup.getLogger(TreeCache.class);

	private static final String ITEM = "item";

	private org.jboss.cache.TreeCache cache;
	private final String regionName;
	private final Fqn regionFqn;
	private final TransactionManager transactionManager;

	public TreeCache(org.jboss.cache.TreeCache cache, String regionName, TransactionManager transactionManager) 
	throws CacheException {
		this.cache = cache;
		this.regionName = regionName;
		this.regionFqn = Fqn.fromString( regionName.replace( '.', '/' ) );
		this.transactionManager = transactionManager;
	}
	
	private Fqn makeFqn(Object key) {
		// HIPPO: generating a flat structure where all children of the region
		//   share a common parent causes very big write locks to be held
		//   when objects are created, which makes deadlocks likely. So, we
		//   go to some effort to make a more "bushy" structure.
		//
		// We know that our key's are one of two things depending on the
		//  entity base class:
		//   [Embedded]GuidPersistable: 14 character strings of base-56 encoded random data
		//   DBUnique: serially increasing longs
		//
		// What we are trading off in the structure of our tree are:
	    //
		//  - The number of times we have to create an intermediate node -
		//    if our tree is too flat, then we'll have to create nodes
		//    frequently, which (at the toplevel) involves locking the
		//    entire tree.
		//  - The overhead from having more intermediate nodes; processing
		//    longer Fqn's is going to be more expensive since we have to
		//    acquire a read lock at every level.
		//  - The number of items per "leaf" - we want to make it unlikely
		//    that we'll be creating an item in a leaf node and writing
		//    to it at the same time.
		// 
		// The current structure with about 16 nodes at each of two levels
		// is just a guess as to what might work well.
		//
		if (key instanceof CacheKey) {
			Object id = ((CacheKey)key).getKey();
			if (id instanceof String) {
				String v = (String)id;
				
				return new Fqn(regionFqn, v.charAt(0) % 16, v.charAt(1) % 16, key);			
			} else if (id instanceof Long) {
				long v = (Long)id;
				
				return new Fqn(regionFqn, v % 13, v % 17, key);
			} 
		}
		
		return new Fqn(regionFqn, key);
	}

	public Object get(Object key) throws CacheException {
		Transaction tx = suspend();
		try {
			return read(key);
		}
		finally {
			resume( tx );
		}
	}
	
	public Object read(Object key) throws CacheException {
		try {
			return cache.get( makeFqn(key), ITEM );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void update(Object key, Object value) throws CacheException {
		try {
			cache.put( makeFqn(key), ITEM, value );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	@SuppressWarnings("deprecation")
	public void put(Object key, Object value) throws CacheException {
		Transaction tx = suspend();
		try {
		    // Workaround for JBCACHE-785
			cache.getInvocationContext().setTransaction(null);
			cache.getInvocationContext().setGlobalTransaction(null);

			//do the failfast put outside the scope of the JTA txn
			cache.putFailFast( makeFqn(key), ITEM, value, 0 );
		}
		catch (TimeoutException te) {
			//ignore!
			log.debug("ignoring write lock acquisition failure");
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
		finally {
			resume( tx );
		}
	}

	private void resume(Transaction tx) {
		try {
			if (tx!=null) transactionManager.resume(tx);
		}
		catch (Exception e) {
			throw new CacheException("Could not resume transaction", e);
		}
	}

	private Transaction suspend() {
		Transaction tx = null;
		try {
			if ( transactionManager!=null ) {
				tx = transactionManager.suspend();
			}
		}
		catch (SystemException se) {
			throw new CacheException("Could not suspend transaction", se);
		}
		return tx;
	}

	public void remove(Object key) throws CacheException {
		try {
			cache.remove( makeFqn(key) );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void clear() throws CacheException {
		try {
			cache.remove( regionFqn );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void destroy() throws CacheException {
		try {
			// NOTE : evict() operates locally only (i.e., does not propogate
			// to any other nodes in the potential cluster).  This is
			// exactly what is needed when we destroy() here; destroy() is used
			// as part of the process of shutting down a SessionFactory; thus
			// these removals should not be propogated
			cache.evict( regionFqn );
		}
		catch( Exception e ) {
			throw new CacheException( e );
		}
	}

	public void lock(Object key) throws CacheException {
		throw new UnsupportedOperationException( "TreeCache is a fully transactional cache" + regionName );
	}

	public void unlock(Object key) throws CacheException {
		throw new UnsupportedOperationException( "TreeCache is a fully transactional cache: " + regionName );
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public int getTimeout() {
		return 600; //60 seconds
	}

	public String getRegionName() {
		return regionName;
	}

	public long getSizeInMemory() {
		return -1;
	}

	public long getElementCountInMemory() {
		try {
			Set children = cache.getChildrenNames( regionFqn );
			return children == null ? 0 : children.size();
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public long getElementCountOnDisk() {
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	private void addToMap(Map map, Fqn parentFqn) throws org.jboss.cache.CacheException {
		Set childrenNames = cache.getChildrenNames( parentFqn );
		if (childrenNames != null) {
			Iterator iter = childrenNames.iterator();
			while ( iter.hasNext() ) {
				Object key = iter.next();
				Fqn fqn = new Fqn(parentFqn, key);
				if (cache.hasChild(fqn))
					addToMap(map, fqn);
				else
					map.put( 
							key, 
							cache.get( fqn, ITEM )
						);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map toMap() {
		try {
			Map result = new HashMap();
			
			addToMap(result, regionFqn);
			
			return result;
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}
	
	@Override
	public String toString() {
		return "TreeCache(" + regionName + ')';
	}
	
}
