package com.dumbhippo.dm.store;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMClassHolder;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.NotCachedException;

public class DMStore {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DMStore.class);
	
	public Map<PropertyKey, DMStoreNode> nodes = new HashMap<PropertyKey, DMStoreNode>();
	
	private synchronized <K, T extends DMObject<K>> DMStoreNode getNode(DMClassHolder classHolder, K key) {
		PropertyKey<K,T> propertyKey = new PropertyKey<K,T>(classHolder, key);
		return nodes.get(propertyKey);
	}
	
	private synchronized <K, T extends DMObject<K>> DMStoreNode ensureNode(DMClassHolder classHolder, K key, int nProperties) {
		PropertyKey<K,T> propertyKey = new PropertyKey<K,T>(classHolder, key);
		DMStoreNode node = nodes.get(propertyKey);
		if (node != null)
			return node;
		
		node = new DMStoreNode(nProperties);
		if (key instanceof DMKey) {
			@SuppressWarnings("unchecked")
			K clonedKey = (K)((DMKey)key).clone();
			propertyKey.setKey(clonedKey); 
		}
		
		nodes.put(propertyKey, node);
		
		return node;
	}
	
	public <K, T extends DMObject<K>> Object fetch(DMClassHolder classHolder, K key, int propertyIndex) throws NotCachedException {
		DMStoreNode node = getNode(classHolder, key);
		if (node == null)
			throw new NotCachedException();
		
		return node.fetch(propertyIndex);
	}

	public <K, T extends DMObject<K>> void store(DMClassHolder classHolder, K key, int propertyIndex, Object value, long timestamp) {
		DMStoreNode node = ensureNode(classHolder, key, classHolder.getPropertyCount());

		node.store(propertyIndex, value, timestamp);
	}

	public <K, T extends DMObject<K>> void invalidate(DMClassHolder classHolder, K key, int propertyIndex, long timestamp) {
		// Note that we need to ensure that the node exists so we have a place to store the timestamp
		// If we were expiring, we could give empty nodes a short expiration time, or even expire them
		// after all sessions (on the cluster) started before their creation were closed.
		DMStoreNode node = ensureNode(classHolder, key, classHolder.getPropertyCount());
		node.invalidate(propertyIndex, timestamp);
	}
	
	private static class PropertyKey<K,T> {
		private DMClassHolder classHolder;
		private K key;

		public PropertyKey(DMClassHolder classHolder , K key) {
			this.classHolder = classHolder;
			this.key = key;
		}
		
		public void setKey(K key) {
			this.key = key;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof PropertyKey))
				return false;
			
			PropertyKey<?,?> other = (PropertyKey<?,?>)o;
			
			return classHolder == other.classHolder &&
				key.equals(other.key);
		}
		
		@Override
		public int hashCode() {
			return 11 * classHolder.hashCode() + 17 * key.hashCode();
		}
	}
}
