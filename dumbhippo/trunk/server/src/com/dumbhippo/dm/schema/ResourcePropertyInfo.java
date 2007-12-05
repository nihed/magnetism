package com.dumbhippo.dm.schema;

import com.dumbhippo.dm.DMObject;

class ResourcePropertyInfo<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> extends PropertyInfo<K,T,TI> {
	private Class<KI> itemKeytype;
	
	public ResourcePropertyInfo(Class<T> declaringType, Class<K> keyType, Class<TI> elementType, Class<KI> itemKeyType) {
		super(declaringType, keyType, elementType);
		this.itemKeytype = itemKeyType;
	}

	@Override
	public DMPropertyHolder<K,T,TI> createPropertyHolder() {
		switch (getContainerType()) {
		case SINGLE:
			return new SingleResourcePropertyHolder<K,T,KI,TI>(this);
		case LIST:
			return new ListResourcePropertyHolder<K,T,KI,TI>(this);
		case SET:
			return new SetResourcePropertyHolder<K,T,KI,TI>(this);
		case FEED:
			return new FeedPropertyHolder<K,T,KI,TI>(this);
		}
		
		throw new RuntimeException("Unexpected container type");
	}

	public Class<KI> getItemKeyType() {
		return itemKeytype;
	}
}
