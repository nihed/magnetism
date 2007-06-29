package com.dumbhippo.persistence;

import java.util.Date;

/**
 * Interface implemented by an entity bean that is a web services cache
 * result, as maintained by AbstractCacheBean.
 * 
 * @author Havoc Pennington
 */
public interface CachedItem {
	public boolean isNoResultsMarker();
	public Date getLastUpdated();
	public void setLastUpdated(Date lastUpdated);
}
