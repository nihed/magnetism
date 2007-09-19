package com.dumbhippo.dm.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javassist.CtMethod;

import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.AndFilter;
import com.dumbhippo.dm.filter.CompiledSetFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;

public class SetResourcePropertyHolder<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> extends ResourcePropertyHolder<K,T,KI,TI> {
	private CompiledSetFilter<K,T,KI,TI> listFilter;

	public SetResourcePropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, DMClassInfo<KI,TI> classInfo, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, classInfo, annotation, filter, viewerDependent);
	}
	
	@Override
	public void complete() {
		super.complete();
		
		Filter classFilter = getResourceClassHolder().getUncompiledItemFilter();
		if (classFilter != null && propertyFilter == null) {
			listFilter = getResourceClassHolder().getSetFilter();
		} else if (classFilter != null || propertyFilter != null) {
			Filter toCompile;
			if (classFilter != null)
				toCompile = new AndFilter(classFilter, propertyFilter);
			else
				toCompile = propertyFilter;
			
			listFilter = FilterCompiler.compileSetFilter(declaringClassHolder.getModel(), 
														  declaringClassHolder.getKeyClass(), 
														  keyType, toCompile);
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
		
		if (filter && listFilter != null)
			itemKeys = listFilter.filterKeys(viewpoint, key, itemKeys);
			
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
		if (listFilter == null)
			return value;
		
		@SuppressWarnings("unchecked")
		Set<TI> items = (Set<TI>)value;

		return listFilter.filterObjects(viewpoint, key, items);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitChildren(DMSession session, Fetch<?,?> children, T object, FetchVisitor visitor) {
		Fetch<KI,TI> typedChildren = (Fetch<KI,TI>)children;
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
