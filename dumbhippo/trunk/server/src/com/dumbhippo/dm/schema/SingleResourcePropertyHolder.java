package com.dumbhippo.dm.schema;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class SingleResourcePropertyHolder<K, T extends DMObject<K>> extends ResourcePropertyHolder<K,T> {
	public SingleResourcePropertyHolder(DMClassHolder<? extends DMObject> declaringClassHolder, CtMethod ctMethod, Class<T> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);
	}

	@Override
	public Object dehydrate(Object value) {
		return dehydrateDMO(value);
	}
	
	@Override
	public Object rehydrate(Object value, DMSession session) {
		return rehydrateDMO(value, session);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void visitChildren(DMSession session, Fetch<?,?> children, DMObject object, FetchVisitor visitor) {
		Fetch<K,T> typedChildren = (Fetch<K,T>)children;
		
		visitChild(session, typedChildren, (T)getRawPropertyValue(object), visitor);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, DMObject object, FetchVisitor visitor) {
		visitResourceValue(session, (T)getRawPropertyValue(object), visitor);
	}
}
