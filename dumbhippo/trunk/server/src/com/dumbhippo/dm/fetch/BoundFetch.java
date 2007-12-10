package com.dumbhippo.dm.fetch;

import java.util.Arrays;
import java.util.Comparator;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.ClientNotificationSet;
import com.dumbhippo.dm.DMClient;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.FeedPropertyHolder;
import com.dumbhippo.dm.store.StoreClient;
import com.dumbhippo.dm.store.StoreKey;

public final class BoundFetch<K,T extends DMObject<K>> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(BoundFetch.class);
	
	private PropertyFetch[] properties;
	private boolean includeDefault;
	
	public BoundFetch(PropertyFetch[] properties, boolean includeDefault) {
		this.properties = properties;
		this.includeDefault = includeDefault;
	}
	
	public PropertyFetch[] getProperties() {
		return properties;
	}
	
	public boolean getIncludeDefault() {
		return includeDefault;
	}
	
	private long propertyOrdering(int i) {
		// We assume that a property ordering of Long.MAX_VALUE will never occur
		return i < properties.length ? properties[i].getProperty().getOrdering() : Long.MAX_VALUE;
	}
	
	private long classOrdering(DMPropertyHolder<?,?,?>[] classProperties, int classIndex) {
		return classIndex < classProperties.length ? classProperties[classIndex].getOrdering() : Long.MAX_VALUE;
	}
	
	private long[] createFeedMinTimestamps(DMClassHolder<?,?> classHolder) {
		long[] feedMinTimestamps = new long[classHolder.getFeedPropertiesCount()];
		for (int i = 0; i < feedMinTimestamps.length; i++)
			feedMinTimestamps[i] = -1;
		
		return feedMinTimestamps;
	}
	
	public <U extends T> void visit(DMSession session, U object, FetchVisitor visitor, boolean indirect) {
		DMClassHolder<K,U> classHolder = object.getClassHolder();
		DMPropertyHolder<K,U,?>[] classProperties = classHolder.getProperties();
		BoundFetch<K,? super U> oldFetch;
		long[] feedMinTimestamps = null;
		
		StoreClient storeClient;
		DMClient client = session.getClient();
		if (client != null)
			storeClient = client.getStoreClient();
		else
			storeClient = null;
		
		boolean needFetch = visitor.getNeedFetch(); 
		
		if (storeClient != null)
			oldFetch = session.getModel().getStore().addRegistration(classHolder, object.getKey(), storeClient, this);
		else
			oldFetch = null;

		boolean allFetched = true;
		boolean noneFetched = true;
		boolean newAnyFetched = false;
		
		int newIndex = 0, oldIndex = 0;
		long newOrdering = propertyOrdering(0);
		long oldOrdering = oldFetch != null ? oldFetch.propertyOrdering(0) : Long.MAX_VALUE;
		for (int classIndex = 0; classIndex < classProperties.length; classIndex++) {
			long classOrdering = classOrdering(classProperties, classIndex);

			while (oldOrdering < classOrdering)
				oldOrdering = oldFetch.propertyOrdering(++oldIndex);
			while (newOrdering < classOrdering)
				newOrdering = propertyOrdering(++newIndex);

			boolean oldFetched = false;
			BoundFetch<?,?> oldChildren = null;
			if (oldOrdering == classOrdering) {
				oldFetched = true; 
				oldChildren = oldFetch.properties[oldIndex].getChildren();
			} else if (oldFetch != null && oldFetch.includeDefault && classProperties[classIndex].getDefaultInclude()) {
				oldFetched = true;
				oldChildren = classProperties[classIndex].getDefaultChildren();
			}
			
			boolean newFetched = false;
			BoundFetch<?,?> newChildren = null;
			if (newOrdering == classOrdering) {
				newFetched = true;
				newChildren = properties[newIndex].getChildren();
			} else if (includeDefault && classProperties[classIndex].getDefaultInclude()) {
				newFetched = true;
				newChildren = classProperties[classIndex].getDefaultChildren();
			}
			
			if (oldFetched || newFetched)
				noneFetched = false;
			else
				allFetched = false;
			
			if (newFetched && !oldFetched)
				newAnyFetched = true;
			
			if ((oldFetched || newFetched) && classProperties[classIndex] instanceof FeedPropertyHolder) {
				@SuppressWarnings("unchecked")
				FeedPropertyHolder<K,T,?,?> property = (FeedPropertyHolder<K,T,?,?>)classProperties[classIndex];

				/* The advantage of always fetching *at least* the default is that 
				 * then we can merge to fetch specifications without knowing the
				 * particular value of the max fetch.
				 */
				int oldMax = 0;
				if (oldOrdering == classOrdering) {
					oldMax = oldFetch.properties[newIndex].getMax();
					if (oldMax < property.getDefaultMaxFetch())
						oldMax = property.getDefaultMaxFetch();
				}
							
				int newMax = 0;
				if (newOrdering == classOrdering) {
					newMax = properties[newIndex].getMax();
					if (newMax < property.getDefaultMaxFetch())
						newMax = property.getDefaultMaxFetch();
				}
				
				if (newMax > oldMax && (newChildren != null || oldChildren != null)) {
					BoundFetch<?,?> children;
					
					if (newChildren == null)
						children = oldChildren;
					else if (oldChildren == null)
						children = newChildren;
					else
						children = newChildren.merge(oldChildren);
					
					if (feedMinTimestamps == null)
						feedMinTimestamps = createFeedMinTimestamps(classHolder);
					int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
									
					feedMinTimestamps[feedPropertyIndex] = property.visitFeedChildren(session, children, oldMax, newMax - oldMax, object, visitor, true);
				} else if (newChildren != null && (oldChildren == null || newChildren != oldChildren)) {
					if (feedMinTimestamps == null)
						feedMinTimestamps = createFeedMinTimestamps(classHolder);
					int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());

					feedMinTimestamps[feedPropertyIndex] = property.visitFeedChildren(session, newChildren, 0, oldMax, object, visitor, false);
				}
			} else if (newChildren != null && (oldChildren == null || newChildren != oldChildren)) {
				classProperties[classIndex].visitChildren(session, newChildren, object, visitor);
			}
		}
		
		// If this resource is part of the direct result, we must always include the resource, 
		//   even if we aren't fetching any properties
		// If we've never told the client about this resource at all, then we must send the
		//   the resource (and in particular, it's class) even if we aren't fetching any properties
		// Otherwise, if there are no new properties to fetch, we are done 
		if (indirect && oldFetch != null && !newAnyFetched)
			return;
		
		String fetchString = null;
		if (needFetch == true) {
			if (noneFetched)
				fetchString = "";
			else if (allFetched)
				fetchString = "*";
			else {
				StringBuilder sb = new StringBuilder();
				
				newIndex = 0; oldIndex = 0;
				newOrdering = propertyOrdering(0);
				oldOrdering = oldFetch != null ? oldFetch.propertyOrdering(0) : Long.MAX_VALUE;
				for (int classIndex = 0; classIndex < classProperties.length; classIndex++) {
					long classOrdering = classOrdering(classProperties, classIndex);

					while (oldOrdering < classOrdering)
						oldOrdering = oldFetch.propertyOrdering(++oldIndex);
					while (newOrdering < classOrdering)
						newOrdering = propertyOrdering(++newIndex);
					
					boolean oldFetched = oldOrdering == classOrdering || (oldFetch != null && oldFetch.includeDefault && classProperties[classIndex].getDefaultInclude());
					boolean newFetched = newOrdering == classOrdering || (includeDefault && classProperties[classIndex].getDefaultInclude());
					
					if (oldFetched || newFetched)
						appendToFetchString(sb, classProperties[classIndex], classHolder.mustQualifyProperty(classIndex));
				}
				
				fetchString = sb.toString();
			}
		}
		
		visitor.beginResource(classHolder, object.getKey(), fetchString, indirect);

		newIndex = 0; oldIndex = 0;
		newOrdering = propertyOrdering(0);
		oldOrdering = oldFetch != null ? oldFetch.propertyOrdering(0) : Long.MAX_VALUE;
		for (int classIndex = 0; classIndex < classProperties.length; classIndex++) {
			long classOrdering = classOrdering(classProperties, classIndex);

			while (oldOrdering < classOrdering)
				oldOrdering = oldFetch.propertyOrdering(++oldIndex);
			while (newOrdering < classOrdering)
				newOrdering = propertyOrdering(++newIndex);
			
			boolean oldFetched = (oldOrdering == classOrdering) || (oldFetch != null && oldFetch.includeDefault && classProperties[classIndex].getDefaultInclude());
			boolean newFetched = (newOrdering == classOrdering) || (includeDefault && classProperties[classIndex].getDefaultInclude());

			if ((oldFetched || newFetched) && classProperties[classIndex] instanceof FeedPropertyHolder) {
				@SuppressWarnings("unchecked")
				FeedPropertyHolder<K,T,?,?> property = (FeedPropertyHolder<K,T,?,?>)classProperties[classIndex];

				int oldMax =0;
				if (oldOrdering == classOrdering) {
					oldMax = oldFetch.properties[newIndex].getMax();
					if (oldMax < property.getDefaultMaxFetch())
						oldMax = property.getDefaultMaxFetch();
				}
							
				int newMax = 0;
				if (newOrdering == classOrdering) {
					newMax = properties[newIndex].getMax();
					if (newMax < property.getDefaultMaxFetch())
						newMax = property.getDefaultMaxFetch();
				}
				
				long minTimestamp;
				if (feedMinTimestamps != null && newMax <= oldMax) {
					int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
					minTimestamp = feedMinTimestamps[feedPropertyIndex];
				} else {
					minTimestamp = 0;
				}

				if (newMax > oldMax)
					property.visitFeedProperty(session, oldMax, newMax - oldMax, object, visitor, minTimestamp);
			} else if (newFetched && !oldFetched) {
				classProperties[classIndex].visitProperty(session, object, visitor, false);
			}
		}
		
		visitor.endResource();
	}
	
	public <U extends T>  void visit(DMSession session, U object, FetchVisitor visitor) {
		visit(session, object, visitor, false);
	}
	
	private void appendToFetchString(StringBuilder sb, DMPropertyHolder<?,?,?> propertyHolder, boolean qualify) {
		if (sb.length() > 0)
			sb.append(";");
		if (qualify)
			sb.append(propertyHolder.getPropertyId());
		else
			sb.append(propertyHolder.getName());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BoundFetch))
			return false;
		
		BoundFetch<?,?> other = (BoundFetch<?,?>)o;
		
		if (includeDefault != other.includeDefault)
			return false;
		
		if (properties.length != other.properties.length)
			return false;
		
		for (int i = 0; i < properties.length; i++)
			if (!properties[i].equals(other.properties[i]))
				return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int value = includeDefault ? 1 : 0;
 
		for (int i = 0; i < properties.length; i++)
			value = value * 31 + properties[i].hashCode();
		
		return value;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		if (includeDefault)
			b.append('+');
		
		// This method is primarily for our tests. For testing purposes, we want a
		// human-predictable ordering for properties, instead of a MD5-hash ordering
		//
		PropertyFetch[] sortedProperties = properties.clone();
		Arrays.sort(sortedProperties, new Comparator<PropertyFetch>() {
			public int compare(PropertyFetch a, PropertyFetch b) {
				return a.getProperty().getPropertyId().compareTo(b.getProperty().getPropertyId());
			}
		});
			
		for (int i = 0; i < sortedProperties.length; i++) {
			if (i != 0 || includeDefault)
				b.append(';');
			b.append(sortedProperties[i].toString());
		}
		
		return b.toString();
	}

	public BoundFetch<?,?> merge(BoundFetch<?,?> other) {
		int newCount = this.properties.length;
		boolean changedProperties = false;

		// Count the total number of properties in the merge of the two fetches
		
		int thisIndex = 0, otherIndex = 0;
		long thisOrdering = this.propertyOrdering(0);
		long otherOrdering = other.propertyOrdering(0);
		while (thisIndex < this.properties.length || otherIndex < other.properties.length) {
			if (thisOrdering < otherOrdering) {
				// Only in this fetch
				thisOrdering = this.propertyOrdering(++thisIndex);
			} else if (thisOrdering == otherOrdering) {
				// In both fetches
				if (!this.properties[thisIndex].equals(other.properties[otherIndex]))
					changedProperties = true;
				thisOrdering = this.propertyOrdering(++thisIndex);
				otherOrdering = other.propertyOrdering(++otherIndex);
			} else {
				// Only in the other fetch
				newCount++;
				otherOrdering = other.propertyOrdering(++otherIndex);
			}
		}
		
		// If the other property is a subset of this one, we can just return this one
		if (!changedProperties && newCount == this.properties.length && (this.includeDefault || !other.includeDefault))
			return this;
		
		PropertyFetch[] newProperties = new PropertyFetch[newCount];
		
		int newIndex = 0;
		thisIndex = 0; otherIndex = 0;
		thisOrdering = this.propertyOrdering(0);
		otherOrdering = other.propertyOrdering(0);
		while (thisIndex < this.properties.length || otherIndex < other.properties.length) {
			if (thisOrdering < otherOrdering) {
				// Only in this fetch
				newProperties[newIndex++] = this.properties[thisIndex];
				thisOrdering = this.propertyOrdering(++thisIndex);
			} else if (thisOrdering == otherOrdering) {
				// In both fetches
				newProperties[newIndex++] = this.properties[thisIndex].merge(other.properties[otherIndex]);
				thisOrdering = this.propertyOrdering(++thisIndex);
				otherOrdering = other.propertyOrdering(++otherIndex);
			} else {
				// Only in the other fetch
				newProperties[newIndex++] = other.properties[otherIndex];
				otherOrdering = other.propertyOrdering(++otherIndex);
			}
		}
		
		@SuppressWarnings("unchecked")
		BoundFetch<?, ?> newFetch = new BoundFetch(newProperties, this.includeDefault || other.includeDefault);
		
		return newFetch;
	}

	public void resolveNotifications(StoreClient client, StoreKey<K,? extends T> key, long propertyMask, ClientNotificationSet result) {
		DMPropertyHolder<K,? extends T,?>[] classProperties = key.getClassHolder().getProperties();
		BoundFetch<?,?>[] childFetches = null;
		int[] maxes = null;
		long notifiedMask = 0;
		
		long bit = 1;
		int classIndex = 0;
		long propertyOrdering = propertyOrdering(0);
		int propertyIndex = 0;
		while (propertyMask != 0) {
			if ((propertyMask & 1) != 0) {
				long classOrdering = classOrdering(classProperties, classIndex);

				while (propertyOrdering < classOrdering)
					propertyOrdering = propertyOrdering(++propertyIndex);
	
				boolean notified = false;
				BoundFetch<?,?> childFetch = null;
				int max = -1;

				if (propertyOrdering == classOrdering) {
					if (properties[propertyIndex].getNotify()) {
						notified = true;
						childFetch = properties[propertyIndex].getChildren();
						max = properties[propertyIndex].getMax();
					}
				} else if (includeDefault && classProperties[classIndex].getDefaultInclude()) {
					notified = true;
					childFetch = classProperties[classIndex].getDefaultChildren();
				}
				
				if (notified)
					notifiedMask |= bit;
				
				if (childFetch != null) {
					if (childFetches == null)
						childFetches = new BoundFetch[classProperties.length];
					childFetches[classIndex] = childFetch;
				}
				
				if (max >= 0) {
					if (maxes == null) {
						maxes = new int[classProperties.length];
						for (int i = 0; i < classProperties.length; i++)
							maxes[i] = -1; 
					}
					maxes[classIndex] = max;
				}
				
			}
			
			propertyMask >>= 1;
			bit <<= 1;
			classIndex++;
		}
		
		if (notifiedMask != 0)
			result.addNotification(client, key, this, notifiedMask, childFetches, maxes);
	}
	
	public <U extends T> String getFetchString(DMClassHolder<K,U> classHolder) {
		DMPropertyHolder<K,U,?>[] classProperties = classHolder.getProperties();
		
		boolean allFetched = true;
		boolean noneFetched = true;
		
		int propertyIndex = 0;
		long propertyOrdering = propertyOrdering(0);
		for (int classIndex = 0; classIndex < classProperties.length; classIndex++) {
			long classOrdering = classOrdering(classProperties, classIndex);

			while (propertyOrdering < classOrdering)
				propertyOrdering = propertyOrdering(++propertyIndex);

			boolean fetch = propertyOrdering == classOrdering || (includeDefault && classProperties[classIndex].getDefaultInclude());
			
			if (fetch)
				noneFetched = false;
			else
				allFetched = false;
		}
		
		if (noneFetched)
			return "";
		else if (allFetched)
			return "*";

		StringBuilder sb = new StringBuilder();
			
		propertyIndex = 0;
		propertyOrdering = propertyOrdering(0);
		for (int classIndex = 0; classIndex < classProperties.length; classIndex++) {
			long classOrdering = classOrdering(classProperties, classIndex);

			while (propertyOrdering < classOrdering)
				propertyOrdering = propertyOrdering(++propertyIndex);
			
			boolean fetch = propertyOrdering == classOrdering || (includeDefault && classProperties[classIndex].getDefaultInclude());
			
			if (fetch)
				appendToFetchString(sb, classProperties[classIndex], classHolder.mustQualifyProperty(classIndex));
		}
			
		return sb.toString();
	}
}
