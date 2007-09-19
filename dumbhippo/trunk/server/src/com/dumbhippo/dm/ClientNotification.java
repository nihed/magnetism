package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMPropertyHolder;
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
	
	public <K, T extends DMObject<K>>void addObjectProperties(StoreKey<K,T> key, Fetch<K,? super T> fetch, long propertyMask, Fetch<?,?>[] childFetches) {
		notifications.add(new ObjectNotification<K,T>(key, fetch, propertyMask, childFetches));
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
		private Fetch<K, ? super T> fetch;
		private long propertyMask;
		private Fetch<?,?>[] childFetches;

		public ObjectNotification(StoreKey<K,T> key, Fetch<K, ? super T> fetch, long propertiesMask, Fetch<?,?>[] childFetches) {
			this.key = key;
			this.fetch = fetch;
			this.propertyMask = propertiesMask;
			this.childFetches = childFetches;
		}
		
		public void visitNotification(DMSession session, FetchVisitor visitor) {
			T object = session.findUnchecked(key);
			DMPropertyHolder<K,T,?>[] classProperties = key.getClassHolder().getProperties();

			long v;
			int propertyIndex;
			
			if (childFetches != null) {
				v = propertyMask;
				propertyIndex = 0;
				while (v != 0) {
					if ((v & 1) != 0 && childFetches[propertyIndex] != null) {
						classProperties[propertyIndex].visitChildren(session, childFetches[propertyIndex], object, visitor);
					}

					v >>= 1;
					propertyIndex++;
				}
			}
			
			visitor.beginResource(key.getClassHolder(), key.getKey(), fetch.getFetchString(key.getClassHolder()), false);
			
			v = propertyMask;
			propertyIndex = 0;
			while (v != 0) {
				if ((v & 1) != 0)
					classProperties[propertyIndex].visitProperty(session, object, visitor, true);

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
