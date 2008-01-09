package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.schema.FeedPropertyHolder;
import com.dumbhippo.dm.store.StoreClient;
import com.dumbhippo.dm.store.StoreKey;

/**
 * ClientNotification stores information about notifications that need to be sent to 
 * a connected client.
 *  
 * @author otaylor
 */
public class ClientNotification {
	private StoreClient client;

	// We keep a flat list here, and don't try to merge together multiple calls to addObjectProperties
	// for the same resource. In the case where notifications are done globally, the merging is
	// done when adding notifications to the ChangeNotificationSet, but if the person calling
	// session.changed() passed in a ClientMatcher, then no merge is done. I think that there
	// shouldn't be much harm for having duplicates of the same resource in the notifications
	// list, but if it proves to be problematical and/or inefficient, we might want to switch
	// to a map instead.
	private List<ObjectNotification<?,?>> notifications = new ArrayList<ObjectNotification<?,?>>();
	
	public ClientNotification(StoreClient client) {
		this.client = client;
	}
	
	public <K, T extends DMObject<K>>void addObjectProperties(StoreKey<K,T> key, BoundFetch<K,? super T> fetch, long propertyMask, BoundFetch<?,?>[] childFetches, int[] maxes) {
		notifications.add(new ObjectNotification<K,T>(key, fetch, propertyMask, childFetches, maxes));
	}
	
	public StoreClient getClient() {
		return client;
	}
	
	public void visitNotification(DMSession session, FetchVisitor visitor) {
		for (ObjectNotification<?,?> notification : notifications)
			notification.visitNotification(session, visitor);
	}
	
	@Override
	public String toString() {
		return "{ client=" + client + ", notifications=" + notifications + " }";
	}

	private static class ObjectNotification<K,T extends DMObject<K>> {
		private StoreKey<K,T> key;
		private BoundFetch<K, ? super T> fetch;
		private long propertyMask;
		private BoundFetch<?,?>[] childFetches;
		private int[] maxes;

		public ObjectNotification(StoreKey<K,T> key, BoundFetch<K, ? super T> fetch, long propertiesMask, BoundFetch<?,?>[] childFetches, int[] maxes) {
			this.key = key;
			this.fetch = fetch;
			this.propertyMask = propertiesMask;
			this.childFetches = childFetches;
			this.maxes = maxes;
		}
		
		private int getMax(FeedPropertyHolder<K,T,?,?> property, int propertyIndex) {
			int max = -1;
			if (maxes != null)
				max = maxes[propertyIndex];
			
			if (max < property.getDefaultMaxFetch())
				max = property.getDefaultMaxFetch();
			
			return max;
		}
		
		public void visitNotification(DMSession session, FetchVisitor visitor) {
			T object = session.findUnchecked(key);
			DMClassHolder<K,T> classHolder = key.getClassHolder();
			DMPropertyHolder<K,T,?>[] classProperties = classHolder.getProperties();
			long feedMinTimestamps[] = null;

			long v;
			int propertyIndex;
			
			if (childFetches != null) {
				v = propertyMask;
				propertyIndex = 0;
				while (v != 0) {
					if ((v & 1) != 0 && childFetches[propertyIndex] != null) {
						if (classProperties[propertyIndex] instanceof FeedPropertyHolder) {
							@SuppressWarnings("unchecked")
							FeedPropertyHolder<K,T,?,?> property = (FeedPropertyHolder<K,T,?,?>)classProperties[propertyIndex];
							
							if (feedMinTimestamps == null) {
								feedMinTimestamps = new long[key.getClassHolder().getFeedPropertiesCount()];
								for (int i = 0; i < feedMinTimestamps.length; i++)
									feedMinTimestamps[i] = -1;
							}
							
							int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
							feedMinTimestamps[feedPropertyIndex] = property.visitFeedChildren(session, childFetches[propertyIndex], 0, getMax(property, propertyIndex), object, visitor, false);
						} else {
							classProperties[propertyIndex].visitChildren(session, childFetches[propertyIndex], object, visitor);
						}
					}

					v >>= 1;
					propertyIndex++;
				}
			}
			
			visitor.beginResource(key.getClassHolder(), key.getKey(), fetch.getFetchString(), false);
			
			v = propertyMask;
			propertyIndex = 0;
			while (v != 0) {
				if ((v & 1) != 0) {
					if (classProperties[propertyIndex] instanceof FeedPropertyHolder) {
						@SuppressWarnings("unchecked")
						FeedPropertyHolder<K,T,?,?> property = (FeedPropertyHolder<K,T,?,?>)classProperties[propertyIndex];

						long minTimestamp = -1;
						if (feedMinTimestamps != null) {
							int feedPropertyIndex = classHolder.getFeedPropertyIndex(property.getName());
							minTimestamp = feedMinTimestamps[feedPropertyIndex];
						}

						property.visitFeedProperty(session, 0, getMax(property, propertyIndex), object, visitor, minTimestamp);
					} else {
						classProperties[propertyIndex].visitProperty(session, object, visitor, true);
					}
				}

				v >>= 1;
				propertyIndex++;
			}
			
			visitor.endResource();
		}
		
		@Override
		public String toString() {
			return "{ " + key + ": propertyMask=" + propertyMask + ", childFetches = " + childFetches + "}";
		}
	}
	
}
