package com.dumbhippo.dm.schema;

import java.util.Collection;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.FilterCompiler;
import com.dumbhippo.dm.store.StoreKey;

public abstract class PlainPropertyHolder<K,T extends DMObject<K>, TI> extends DMPropertyHolder<K,T,TI> {
	protected CompiledItemFilter<K,T,Object,DMObject<Object>> itemFilter;
	private PropertyType propertyType;
	
	public PlainPropertyHolder(PropertyInfo<K,T,TI> propertyInfo) {
		super(propertyInfo);

		if (propertyFilter != null)
			itemFilter = FilterCompiler.compileItemFilter(getModel(), 
			 										      propertyInfo.getKeyType(), 
													      Object.class, propertyFilter);
		
		PropertyType derivedType;
		if (elementType.isPrimitive()) {
			if (elementType == Boolean.TYPE)
				derivedType = PropertyType.BOOLEAN;
			else if (elementType == Byte.TYPE ||
					 elementType == Character.TYPE ||
					 elementType == Short.TYPE ||
					 elementType == Integer.TYPE)
				derivedType = PropertyType.INTEGER;
			else if (elementType == Long.TYPE)
				derivedType = PropertyType.LONG;
			else if (elementType == Float.TYPE ||
					 elementType == Double.TYPE)
				derivedType = PropertyType.FLOAT;
			else
				throw new RuntimeException("Unexpected primitive type" + elementType);
		} else {
			if (elementType == String.class)
				derivedType = PropertyType.STRING;
			else if (elementType == StoreKey.class)
				derivedType = PropertyType.STORE_KEY;
			else
				throw new RuntimeException("Unexpected type" + elementType);
		}
		
		if (annotation.type() == PropertyType.AUTO)
			propertyType = derivedType;
		else {
			switch (annotation.type()) {
			case AUTO: // not reached
				break;
			case BOOLEAN:
			case INTEGER:
			case LONG:				
			case FLOAT:
			case STRING:
				if (annotation.type() != derivedType)
					throw new RuntimeException("type=PropertyType." + annotation.type() + " found but expected " + derivedType + " from the return type"); 
				break;
			case URL:
				if (derivedType != PropertyType.STRING)
					throw new RuntimeException("PropertyType.URL for non-string property");
				break;
			case STORE_KEY:
				if (derivedType != PropertyType.STORE_KEY)
					throw new RuntimeException("PropertyType.STORE_KEY for non-store-key property");
				break;
			case RESOURCE:
				throw new RuntimeException("PropertyType.RESOURCE for non-resource property");
			case FEED:
				throw new RuntimeException("PropertyType.RESOURCE for non-resource property");
			}
			
			propertyType = annotation.type();
		}
	}

	@Override
	protected PropertyType getType() {
		return propertyType;
	}
	
	public Class<?> getPlainType() {
		return elementType;
	}
	
	@Override
	public Object dehydrate(Object value) {
		if (propertyType == PropertyType.STORE_KEY)
			return ((StoreKey)value).clone();
		else
			return value;
	}
	
	@Override
	public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session, boolean filter) {
		if (value == null)
			return null;
		else if (filter)
			return filter(viewpoint, key, value);
		else
			return value;
	}

	@Override
	public BoundFetch<?,?> getDefaultChildren() {
		return null;
	}
	
	@Override
	public String getDefaultChildrenString() {
		return null;
	}

	@Override
	public void visitChildren(DMSession session, BoundFetch<?, ?> children, T object, FetchVisitor visitor) {
	}
	
	protected void visitPlainValues(DMSession session, Collection<TI> values, FetchVisitor visitor) {
		if (values.isEmpty()) {
			visitor.emptyProperty(this);
		} else {
			for (TI value : values)
				visitor.plainProperty(this, value);
		}
	}
}
