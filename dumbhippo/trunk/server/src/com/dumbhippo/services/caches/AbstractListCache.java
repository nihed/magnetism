package com.dumbhippo.services.caches;

import java.util.List;

/** 
 * Interface extended by interfaces that cache a web service request with a list-of-items result.
 *  
 * @author Havoc Pennington
 *
 * @param <KeyType> web service request arg
 * @param <ResultType> type of a single item in the list returned by the web service
 */
public interface AbstractListCache<KeyType, ResultType> extends AbstractCache<KeyType, List<ResultType>> {

	public void deleteCache(KeyType key);
}
