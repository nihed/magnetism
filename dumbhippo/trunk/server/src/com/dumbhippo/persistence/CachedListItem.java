package com.dumbhippo.persistence;

import java.util.Date;

/**
 * Interface for an entity bean that is one list item in 
 * a list of cached results, as manipulated by AbstractListCacheWithStorageBean.
 * 
 * @author Havoc Pennington
 *
 */
public interface CachedListItem {
	public boolean isNoResultsMarker();
	public Date getLastUpdated();
	public void setLastUpdated(Date lastUpdated);
}
