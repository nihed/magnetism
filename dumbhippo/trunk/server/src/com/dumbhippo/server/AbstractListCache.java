package com.dumbhippo.server;

import java.util.List;
import java.util.concurrent.Future;

/** 
 * Interface extended by interfaces that cache a web service request with a list-of-items result.
 *  
 * @author Havoc Pennington
 *
 * @param <KeyType> web service request arg
 * @param <ResultType> type of a single item in the list returned by the web service
 */
public interface AbstractListCache<KeyType, ResultType> {

	public List<ResultType> getSync(KeyType key);

	public Future<List<ResultType>> getAsync(KeyType key);

	public List<ResultType> checkCache(KeyType key);

	public List<ResultType> fetchFromNet(KeyType key);

	public List<ResultType> saveInCache(KeyType key, List<ResultType> newResults);
}
