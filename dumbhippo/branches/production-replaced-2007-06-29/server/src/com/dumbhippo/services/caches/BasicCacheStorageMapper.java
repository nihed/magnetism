package com.dumbhippo.services.caches;

import com.dumbhippo.persistence.CachedItem;

/** Maps from a web service result to a single database entity (one row per result to be saved).
 */
public interface BasicCacheStorageMapper<KeyType,ResultType,EntityType extends CachedItem> {

	public EntityType newNoResultsMarker(KeyType key);
	
	public EntityType queryExisting(KeyType key);
	
	public ResultType resultFromEntity(EntityType entity);
	
	public EntityType entityFromResult(KeyType key, ResultType result);
	
	public void updateEntityFromResult(KeyType key, ResultType result, EntityType entity);
}
