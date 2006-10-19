package com.dumbhippo.server.views;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.VersionedEntity;

/* Abstract superclass of PersonView, GroupView which holds
 * utility methods useful to both.
 */
public abstract class EntityView implements ObjectView {
	
	protected abstract VersionedEntity getVersionedEntity();
	
	public abstract String getName();
	
	public abstract String getHomeUrl();
	
	public abstract String getPhotoUrl();
	
	public static String sizePhoto(String baseUrl, int size) {
		int lastSlash = baseUrl.lastIndexOf('/');
		StringBuilder newUrl = new StringBuilder(baseUrl.substring(0, lastSlash+1));
		newUrl.append(""+size);
		newUrl.append(baseUrl.substring(lastSlash));
		return newUrl.toString();
	}
	
	public String getPhotoUrl(int size) {
		return sizePhoto(getPhotoUrl(), size);
	}

	public String getPhotoUrl30() {
		return getPhotoUrl(30);
	}

	public String getPhotoUrl60() {
		return getPhotoUrl(60);
	}
	
	public abstract Guid getIdentifyingGuid();
	
	public abstract void writeToXmlBuilderOld(XmlBuilder builder);
	
	public abstract String toIdentifyingXml();
	
	// final since changing this would break the guarantee that it 
	// returns the same thing as writeToXmlBuilderOld 
	final public String toXmlOld() {
		XmlBuilder xml = new XmlBuilder();
		writeToXmlBuilderOld(xml);
		return xml.toString();
	}
	
	public List<Object> getReferencedObjects() {
		return Collections.emptyList();
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
