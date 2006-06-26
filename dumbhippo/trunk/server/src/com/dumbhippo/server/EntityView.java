package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.VersionedEntity;

/* Abstract superclass of PersonView, GroupView which holds
 * utility methods useful to both.
 */
public abstract class EntityView {
	
	protected abstract VersionedEntity getVersionedEntity();
	
	public abstract String getName();
	
	public abstract String getHomeUrl();
	
	public abstract String getSmallPhotoUrl();
	
	public abstract Guid getIdentifyingGuid();
	
	public abstract String toXml();
	
	public abstract String toIdentifyingXml();
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EntityView))
			return false;
		EntityView obj = (EntityView) o;
		return obj.getIdentifyingGuid().equals(getIdentifyingGuid());
	}

	@Override
	public int hashCode() {
		return getIdentifyingGuid().hashCode();
	}		
}
