package com.dumbhippo.dm.schema;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;

public abstract class PlainPropertyHolder extends DMPropertyHolder {
	public PlainPropertyHolder(DMClassHolder<? extends DMObject> declaringClassHolder, CtMethod ctMethod, Class<?> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);
	}

	public Class<?> getPlainType() {
		return elementType;
	}
	
	@Override
	public Object dehydrate(Object value) {
		return value;
	}
	
	@Override
	public Object rehydrate(Object value, DMSession session) {
		return value;
	}

	@Override
	public Fetch<?,?> getDefaultChildren() {
		return null;
	}

	@Override
	public void visitChildren(DMSession session, Fetch<?, ?> children, DMObject object, FetchVisitor visitor) {
	}
}
