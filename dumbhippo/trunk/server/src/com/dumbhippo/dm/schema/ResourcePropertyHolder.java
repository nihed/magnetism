package com.dumbhippo.dm.schema;

import java.util.Collection;

import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.identity20.Guid;

public abstract class ResourcePropertyHolder<K,T extends DMObject<K>, KI,TI extends DMObject<KI>> extends DMPropertyHolder<K,T,TI> {
	private DMClassHolder<KI,TI> resourceClassHolder;
	protected Class<TI> itemObjectType;
	protected Class<KI> itemKeyType;
	private BoundFetch<KI,TI> defaultChildren;

	public ResourcePropertyHolder(ResourcePropertyInfo<K,T,KI,TI> propertyInfo) {
		super(propertyInfo);
		itemObjectType = propertyInfo.getItemType();
		itemKeyType = propertyInfo.getItemKeyType();
		
		if (annotation.type() != PropertyType.AUTO && annotation.type() != PropertyType.RESOURCE) {
			throw new RuntimeException("type=PropertyType." + annotation.type() + " found for a property with a resource return type"); 
		}
	}
	
	@Override
	public void complete() {
		if (completed)
			return;

		super.complete();
		
		resourceClassHolder = getModel().getClassHolder(itemKeyType, getResourceType());

		if (!"".equals(annotation.defaultChildren())) {
			defaultInclude = true;
			
			try {
				FetchNode node = FetchParser.parse(annotation.defaultChildren());
				if (node.getProperties().length > 0) {
					defaultChildren = node.bind(resourceClassHolder);
				}
			} catch (ParseException e) {
				throw new RuntimeException(propertyId + ": failed to parse defaultChildren", e);
			}
		}
	}

	@Override
	protected PropertyType getType() {
		return PropertyType.RESOURCE;
	}

	@Override
	public BoundFetch<KI,TI> getDefaultChildren() {
		return defaultChildren;
	}
	
	@Override
	public String getDefaultChildrenString() {
		if ("".equals(annotation.defaultChildren()))
			return null;
		else
			return annotation.defaultChildren();
	}
	
	@SuppressWarnings("unchecked")
	public Class<TI> getResourceType() {
		return elementType;
	}
	
	public DMClassHolder<KI,TI> getResourceClassHolder() {
		if (completed)
			return resourceClassHolder;
		else {
			@SuppressWarnings("unchecked")
			DMClassHolder<KI,TI> classHolder = getModel().getClassHolder(itemKeyType, getResourceType()); 
			return classHolder;
		}
	}
	
	protected Object dehydrateDMO(Object value) {
		Object key = ((DMObject<?>)value).getKey();
		if ((key instanceof Guid) || (key instanceof String) || (key instanceof Long)) {
			return key;
		} else {
			return ((DMKey)key).clone();
		}
	}

	@SuppressWarnings("unchecked")
	public TI rehydrateDMO(Object value, DMSession session) {
		@SuppressWarnings("unchecked")
		KI key = (KI)value;
		return session.findUnchecked(itemObjectType, key);
	}
	
	protected void visitChild(DMSession session, BoundFetch<KI,TI> children, TI value, FetchVisitor visitor) {
		children.visit(session, value, visitor, true);
	}

	protected void visitResourceValue(DMSession session, TI value, FetchVisitor visitor) {
		visitor.resourceProperty(this, value.getKey());
	}

	protected void visitResourceValues(DMSession session, Collection<? extends TI> values, FetchVisitor visitor) {
		if (values.isEmpty()) {
			visitor.emptyProperty(this);
		} else {
			for (TI value : values)
				visitor.resourceProperty(this, value.getKey());
		}
	}

	@Override
	public Class<?> getKeyClass() {
		return resourceClassHolder.getKeyClass();
	}
}
