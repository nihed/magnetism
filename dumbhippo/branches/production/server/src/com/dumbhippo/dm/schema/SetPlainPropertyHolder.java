package com.dumbhippo.dm.schema;

import java.util.Collections;
import java.util.Set;

import javassist.CtMethod;

import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class SetPlainPropertyHolder<K, T extends DMObject<K>, TI>  extends PlainPropertyHolder<K,T,TI> {
	public SetPlainPropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, Class<TI> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);
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
	public Object filter(DMViewpoint viewpoint, K key, Object value) {
		if (itemFilter == null)
			return value;
		
		// We cheat here to filter a non-key value, because we never dereference
		Object result = itemFilter.filterKey(viewpoint, key, value);
		if (result == null)
			return Collections.emptySet();
		else
			return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, T object, FetchVisitor visitor, boolean forceEmpty) {
		visitPlainValues(session, (Set<TI>)getRawPropertyValue(object), visitor);
	}
	
	@Override
	public Cardinality getCardinality() {
		return Cardinality.ANY;
	}
}
