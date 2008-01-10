package com.dumbhippo.services.caches;

import java.util.List;

/** 
 * Maps from a web service request result to multiple entities (a list of database rows).
 * 
 */
public interface ListCacheStorageMapper<KeyType,ResultType,EntityType> {
	public EntityType newNoResultsMarker(KeyType key);

	public void setAllLastUpdatedToZero(KeyType key);

	public List<EntityType> queryExisting(KeyType key);
	
	public ResultType resultFromEntity(EntityType entity);
	
	public EntityType entityFromResult(KeyType key, ResultType result);
}
