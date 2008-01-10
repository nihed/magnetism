package com.dumbhippo.search;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.util.EJBUtil;

/**
 * This implements a custom backend for hibernate-search. It's quite similar 
 * to the standard "jms" backend; the main points of difference are:
 *  
 *  - We use our JMS-wrapper infrastructure when queueing messages
 *  
 *  - We put all indexing requests through JMS and don't short-circuit
 *    the ones on the indexing node; this is necessary because for us
 *    the indexing node is dynamically chosen as a singleton MBean
 *    
 *  - We support messages in the queue that trigger clearing the
 *    existing queue. This allows a reindex without having to stop
 *    the cluster and manually delete the old indices.
 *    
 * Before we switched from the old search support in hibernate-annotations,
 * to hibernate-search, we used to use the transactional capability of
 * JMS to cause reindexing to happen on commit, but that doesn't make
 * sense here because hibernate-search wraps up all the changes from
 * a single transaction into a List&lt;LuceneWork&gt; and then calls
 * us from a synchronization on commit. 
 */
public class IndexProcessor implements Runnable {
	static private final Logger logger = GlobalSetup.getLogger(IndexProcessor.class);
	List<LuceneWork> queue;
	
	public IndexProcessor(List<LuceneWork> queue) {
		this.queue = queue;
	}
	
	public void run() {
		logger.debug("Inserting {} LuceneWork into JMS index queue", queue.size());
		
		try {
			EJBUtil.defaultLookup(SearchSystem.class).queueIndexWork(queue);
		} catch (RuntimeException e) {
			logger.error("Error putting work items into JMS index queue", e);
		}
	}
}
