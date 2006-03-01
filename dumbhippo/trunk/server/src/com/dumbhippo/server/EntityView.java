package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.VersionedEntity;

/* Abstract superclass of PersonView, GroupView which holds
 * utility methods useful to both.
 */
public abstract class EntityView {
	
	protected abstract VersionedEntity getVersionedEntity();
	protected abstract String getFilePath();
	
	private String getPhotoUrl(VersionedEntity entity, int size) {
		if (entity == null)
			return null;
		StringBuilder sb = new StringBuilder("/files");
		sb.append(getFilePath());
		sb.append("/");
		sb.append(size);
		sb.append("/");
		sb.append(entity.getId());
		sb.append("?v=");
		sb.append(entity.getVersion());
		return sb.toString();
	}
	
	public String getSmallPhotoUrl() {
		return getPhotoUrl(getVersionedEntity(), Configuration.SHOT_SMALL_SIZE);
	}

	public String getLargePhotoUrl() {
		return getPhotoUrl(getVersionedEntity(), Configuration.SHOT_LARGE_SIZE);
	}	
	
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
