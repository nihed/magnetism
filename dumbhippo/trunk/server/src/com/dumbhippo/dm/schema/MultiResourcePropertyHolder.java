package com.dumbhippo.dm.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.AndFilter;
import com.dumbhippo.dm.filter.CompiledListFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;

public class MultiResourcePropertyHolder<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> extends ResourcePropertyHolder<K,T,KI,TI> {
	private CompiledListFilter<K,T,KI,TI> listFilter;

	public MultiResourcePropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, DMClassInfo<KI,TI> classInfo, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, classInfo, annotation, filter, viewerDependent);
	}
	
	@Override
	public void complete() {
		super.complete();
		
		Filter classFilter = getResourceClassHolder().getUncompiledItemFilter();
		if (classFilter != null && propertyFilter == null) {
			listFilter = getResourceClassHolder().getListFilter();
		} else if (classFilter != null || propertyFilter != null) {
			Filter toCompile;
			if (classFilter != null)
				toCompile = new AndFilter(classFilter, propertyFilter);
			else
				toCompile = propertyFilter;
			
			listFilter = FilterCompiler.compileListFilter(declaringClassHolder.getModel(), 
														  declaringClassHolder.getKeyClass(), 
														  keyType, toCompile);
		}
	}

	@Override
	public String getUnboxPrefix() {
		return "(java.util.List)";
	}
	
	@Override
	public String getUnboxSuffix() {
		return "";
	}

	@Override
	public Object dehydrate(Object value) {
		List<Object> result = new ArrayList<Object>();
		for (Object o : (Collection<?>)value)
			result.add(dehydrateDMO(o));
		
		return result;
	}
	
	@Override
	public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session) {
		@SuppressWarnings("unchecked")
		List<KI> itemKeys= (List<KI>)value;
		
		if (listFilter != null)
			itemKeys = listFilter.filterKeys(viewpoint, key, itemKeys);
			
		if (itemKeys.isEmpty())
			return Collections.emptyList();

		List<TI> result = new ArrayList<TI>();

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
		List<TI> items = (List<TI>)value;

		return listFilter.filterObjects(viewpoint, key, items);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitChildren(DMSession session, Fetch<?,?> children, T object, FetchVisitor visitor) {
		Fetch<KI,TI> typedChildren = (Fetch<KI,TI>)children;
		for (TI value : (List<TI>)getRawPropertyValue(object)) {
			visitChild(session, typedChildren, value, visitor);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, T object, FetchVisitor visitor) {
		for (DMObject value : (List<? extends DMObject>)getRawPropertyValue(object)) {
			visitResourceValue(session, (TI)value, visitor);
		}
	}

}
