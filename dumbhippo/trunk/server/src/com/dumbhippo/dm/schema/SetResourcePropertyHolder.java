package com.dumbhippo.dm.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.AndFilter;
import com.dumbhippo.dm.filter.CompiledSetFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;

public class SetResourcePropertyHolder<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> extends ResourcePropertyHolder<K,T,KI,TI> {
	private CompiledSetFilter<K,T,KI,TI> setFilter;

	public SetResourcePropertyHolder(ResourcePropertyInfo<K,T,KI,TI> propertyInfo) {
		super(propertyInfo);
	}
	
	@Override
	public void complete() {
		super.complete();
		
		Filter classFilter = getResourceClassHolder().getUncompiledItemFilter();
		if (classFilter != null && propertyFilter == null) {
			setFilter = getResourceClassHolder().getSetFilter();
		} else if (classFilter != null || propertyFilter != null) {
			Filter toCompile;
			if (classFilter != null)
				toCompile = new AndFilter(classFilter, propertyFilter);
			else
				toCompile = propertyFilter;
			
			setFilter = FilterCompiler.compileSetFilter(getModel(), 
														propertyInfo.getKeyType(), 
														itemKeyType, toCompile);
		}
	}

	@Override
	public String getUnboxPrefix() {
		return "(java.util.Set)";
	}
	
	@Override
	public String getUnboxSuffix() {
		return "";
	}

	@Override
	public Object dehydrate(Object value) {
		Set<Object> result = new HashSet<Object>();
		for (Object o : (Collection<?>)value)
			result.add(dehydrateDMO(o));
		
		return result;
	}
	
	@Override
	public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session, boolean filter) {
		@SuppressWarnings("unchecked")
		Set<KI> itemKeys= (Set<KI>)value;
		
		if (filter && setFilter != null)
			itemKeys = setFilter.filterKeys(viewpoint, key, itemKeys);
			
		if (itemKeys.isEmpty())
			return Collections.emptySet();

		Set<TI> result = new HashSet<TI>();

		for (KI itemKey : itemKeys) {
			result.add(rehydrateDMO(itemKey, session));
		}
		
		return result;
	}
	
	@Override
	public Object filter(DMViewpoint viewpoint, K key, Object value) {
		if (setFilter == null)
			return value;
		
		@SuppressWarnings("unchecked")
		Set<TI> items = (Set<TI>)value;

		return setFilter.filterObjects(viewpoint, key, items);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitChildren(DMSession session, BoundFetch<?,?> children, T object, FetchVisitor visitor) {
		BoundFetch<KI,TI> typedChildren = (BoundFetch<KI,TI>)children;
		for (TI value : (Set<TI>)getRawPropertyValue(object)) {
			visitChild(session, typedChildren, value, visitor);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, T object, FetchVisitor visitor, boolean forceEmpty) {
		visitResourceValues(session, (Set<? extends TI>)getRawPropertyValue(object), visitor);
	}
	
	@Override
	public Cardinality getCardinality() {
		return Cardinality.ANY;
	}
}
