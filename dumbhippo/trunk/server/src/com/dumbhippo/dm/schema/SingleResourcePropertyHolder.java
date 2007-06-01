package com.dumbhippo.dm.schema;

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
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;

public class SingleResourcePropertyHolder<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> extends ResourcePropertyHolder<K,T,KI,TI> {
	private CompiledItemFilter<K,T,KI,TI> itemFilter;

	public SingleResourcePropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, DMClassInfo<KI,TI> classInfo, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, classInfo, annotation, filter, viewerDependent);
	}

	@Override
	public void complete() {
		if (completed)
			return;
		
		super.complete();
		
		Filter classFilter = getResourceClassHolder().getUncompiledItemFilter();
		if (classFilter != null && propertyFilter == null) {
			itemFilter = getResourceClassHolder().getItemFilter();
		} else if (classFilter != null || propertyFilter != null) {
			Filter toCompile;
			if (classFilter != null)
				toCompile = new AndFilter(classFilter, propertyFilter);
			else
				toCompile = propertyFilter;
			
			itemFilter = FilterCompiler.compileItemFilter(declaringClassHolder.getModel(), 
														  declaringClassHolder.getKeyClass(), 
														  keyType, toCompile);
		}
	}

	@Override
	public Object dehydrate(Object value) {
		return dehydrateDMO(value);
	}
	
	@Override
	public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session) {
		if (itemFilter == null)
			return rehydrateDMO(value, session);
		
		@SuppressWarnings("unchecked")
		KI itemKey = (KI)value;
		KI filtered = itemFilter.filterKey(viewpoint, key, itemKey);
		if (filtered != null)
			return rehydrateDMO(filtered, session);
		else
			return null;
	}
	

	@Override
	public Object filter(DMViewpoint viewpoint, K key, Object value) {
		if (itemFilter == null)
			return value;
		
		@SuppressWarnings("unchecked")
		TI item = (TI)value;
		return itemFilter.filterObject(viewpoint, key, item);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitChildren(DMSession session, Fetch<?,?> children, T object, FetchVisitor visitor) {
		Fetch<KI,TI> typedChildren = (Fetch<KI,TI>)children;

		TI value = (TI)getRawPropertyValue(object);
		if (value != null)
			visitChild(session, typedChildren, value, visitor);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, T object, FetchVisitor visitor) {
		TI value = (TI)getRawPropertyValue(object);
		if (value != null)
			visitResourceValue(session, value, visitor);
	}
}
