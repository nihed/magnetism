package com.dumbhippo.server;

import com.dumbhippo.XmlBuilder;
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
	
	public abstract void writeToXmlBuilder(XmlBuilder builder);
	
	public abstract String toIdentifyingXml();
	
	// final since changing this would break the guarantee that it 
	// returns the same thing as writeToXmlBuilder 
	final public String toXml() {
		XmlBuilder xml = new XmlBuilder();
		writeToXmlBuilder(xml);
		return xml.toString();
	}
	
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
