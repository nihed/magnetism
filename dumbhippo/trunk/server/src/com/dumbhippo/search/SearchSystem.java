package com.dumbhippo.search;

import java.io.IOException;
import java.util.List;

import javax.ejb.Local;

import org.apache.lucene.search.Hits;
import org.hibernate.search.backend.LuceneWork;

@Local
public interface SearchSystem {
	/**
	 * Search the given entity with the given query
	 * 
	 * @param entity the entity class to search entities in
	 * @param query the query to search the entities with
	 */
	Hits search(Class<?> clazz, String[] fields, String queryString) throws IOException, org.apache.lucene.queryParser.ParseException;
	
	/**
	 * Asynchronously reindex all indexed items
	 * 
	 * @param what a comma-separated list of unqualified entity class type
	 *   names to reindex, or null to reindex everything.
	 */
	void reindexAll(String what);
	
	/////////////////////////////////////////////////////////////////////
	//
	// Internals
	//
	/////////////////////////////////////////////////////////////////////
	
	/**
	 * Method called to queue index work over jms
	 * 
	 * @param queue items to index
	 */
	void queueIndexWork(List<LuceneWork> queue);
	
	/**
	 * Method called when we get a queued index method over JMS 
	 * 
	 * @param queue items to index
	 */
	void doIndexWork(List<LuceneWork> queue);
	
	/**
	 * Method called when we get a request to clear an index over JMS 
	 * 
	 * @param class for which to clear the index
	 */
	void clearIndex(Class<?> clazz);
}
