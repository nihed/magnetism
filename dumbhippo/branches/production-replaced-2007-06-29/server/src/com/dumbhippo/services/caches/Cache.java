package com.dumbhippo.services.caches;

import java.util.concurrent.Future;

/**
 * Interface extended by session bean interfaces representing a web service cached
 * in a database table.
 * 
 * The key and result should not be entity beans; they should be what the web service 
 * takes and returns, i.e. the same thing they would be if no database cache was involved.
 * 
 * @author Havoc Pennington
 *
 * @param <KeyType> the argument to the web service request (should not be an entity bean)
 * @param <ResultType> the result of the web service request (should not be an entity bean)
 */
public interface Cache<KeyType, ResultType> extends CacheStorage<KeyType, ResultType> {

	public ResultType getSync(KeyType key);

	public Future<ResultType> getAsync(KeyType key);

	public ResultType getSync(KeyType key, boolean alwaysRefetchEvenIfCached);

	public Future<ResultType> getAsync(KeyType key, boolean alwaysRefetchEvenIfCached);	
	
	public ResultType fetchFromNet(KeyType key);
	
	public ResultType saveInCache(KeyType key, ResultType result, boolean refetchedWithoutCheckingCache);
}
