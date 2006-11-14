package com.dumbhippo.services.caches;

import java.util.Date;
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
public interface AbstractCache<KeyType, ResultType> {

	public ResultType getSync(KeyType key);

	public Future<ResultType> getAsync(KeyType key);

	// null return means "we cached the lack of a result", while throwing NotCachedException
	// means "we have nothing cached". Subclasses of AbstractListCache never return null,
	// they return empty list for "we cached the lack of a result"
	public ResultType checkCache(KeyType key) throws NotCachedException;

	public ResultType fetchFromNet(KeyType key);

	public ResultType saveInCache(KeyType key, ResultType newResult);

	/** It's wrong to use this unless it's from inside another saveInCache* call, because 
	 * 	the results you're saving should have been obtained outside a transaction.
	 */
	public ResultType saveInCacheInsideExistingTransaction(KeyType key, ResultType data, Date now);	
	
	public void expireCache(KeyType key);
}
