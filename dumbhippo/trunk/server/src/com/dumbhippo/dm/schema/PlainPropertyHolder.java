package com.dumbhippo.dm.schema;

import java.util.Collection;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.FilterCompiler;

public abstract class PlainPropertyHolder<K,T extends DMObject<K>, TI> extends DMPropertyHolder<K,T,TI> {
	protected CompiledItemFilter<K,T,Object,DMObject<Object>> itemFilter;
	
	public PlainPropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, Class<TI> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);

		if (propertyFilter != null)
			itemFilter = FilterCompiler.compileItemFilter(declaringClassHolder.getModel(), 
			 										      declaringClassHolder.getKeyClass(), 
													      Object.class, propertyFilter);
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
