package com.dumbhippo.dm.schema;

import java.util.Collection;
import java.util.Date;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.FilterCompiler;

public abstract class PlainPropertyHolder<K,T extends DMObject<K>, TI> extends DMPropertyHolder<K,T,TI> {
	protected CompiledItemFilter<K,T,Object,DMObject<Object>> itemFilter;
	private PropertyType propertyType;
	
	public PlainPropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, Class<TI> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);

		if (propertyFilter != null)
			itemFilter = FilterCompiler.compileItemFilter(declaringClassHolder.getModel(), 
			 										      declaringClassHolder.getKeyClass(), 
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
			case RESOURCE:
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
		return value;
	}
	
	@Override
	public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session) {
		return filter(viewpoint, key, value);
	}

	@Override
	public Fetch<?,?> getDefaultChildren() {
		return null;
	}
	
	@Override
	public String getDefaultChildrenString() {
		return null;
	}

	@Override
	public void visitChildren(DMSession session, Fetch<?, ?> children, T object, FetchVisitor visitor) {
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
