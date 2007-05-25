package com.dumbhippo.dm.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class MultiResourcePropertyHolder<K, T extends DMObject<K>> extends ResourcePropertyHolder<K,T> {
	public MultiResourcePropertyHolder(DMClassHolder<? extends DMObject> declaringClassHolder, CtMethod ctMethod, Class<T> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);
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
	public Object rehydrate(Object value, DMSession session) {
		List<Object> result = new ArrayList<Object>();
		for (Object o : (Collection<?>)value) {
			result.add(rehydrateDMO(o, session));
		}
		
		return result;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void visitChildren(DMSession session, Fetch<?,?> children, DMObject object, FetchVisitor visitor) {
		Fetch<K,T> typedChildren = (Fetch<K,T>)children;
		for (T value : (List<T>)getRawPropertyValue(object)) {
			visitChild(session, typedChildren, value, visitor);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, DMObject object, FetchVisitor visitor) {
		for (DMObject value : (List<? extends DMObject>)getRawPropertyValue(object)) {
			visitResourceValue(session, (T)value, visitor);
		}
	}
}
