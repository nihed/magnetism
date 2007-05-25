package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMPropertyHolder;
import com.dumbhippo.dm.store.StoreClient;
import com.dumbhippo.dm.store.StoreKey;

public class ClientNotification {
	private StoreClient client;
	private List<ObjectNotification> notifications = new ArrayList<ObjectNotification>();
	
	public ClientNotification(StoreClient client) {
		this.client = client;
	}
	
	public <K, T extends DMObject<K>>void addObjectProperties(StoreKey<K,T> key, Fetch<K,? super T> fetch, long propertyMask, Fetch[] childFetches) {
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
		private Fetch[] childFetches;

		public ObjectNotification(StoreKey<K,T> key, Fetch<K, ? super T> fetch, long propertiesMask, Fetch[] childFetches) {
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
					classProperties[propertyIndex].visitProperty(session, object, visitor);

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
