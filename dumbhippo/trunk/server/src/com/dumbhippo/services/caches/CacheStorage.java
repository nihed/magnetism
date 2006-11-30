package com.dumbhippo.services.caches;

import java.util.Date;

/** 
 * This interface is extended by the cache bean interfaces, but also implemented by 
 * non-session-bean cache storage objects.
 * 
 * @author Havoc Pennington
 *
 * @param <KeyType>
 * @param <ResultType>
 */
public interface CacheStorage<KeyType, ResultType> {

	// null return means "we cached the lack of a result", while throwing NotCachedException
	// means "we have nothing cached". Subclasses of ListCache never return null,
	// they return empty list for "we cached the lack of a result"
	public ResultType checkCache(KeyType key) throws NotCachedException;

	/** It's wrong to use this unless it's from inside another saveInCache* call, because 
	 * 	the results you're saving should have been obtained outside a transaction.
	 */
	public ResultType saveInCacheInsideExistingTransaction(KeyType key,
			ResultType data, Date now);

	public void expireCache(KeyType key);

}