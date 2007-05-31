package com.dumbhippo.dm.schema;

import java.util.Collections;
import java.util.List;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class ListPlainPropertyHolder<K, T extends DMObject<K>, TI>  extends PlainPropertyHolder<K,T,TI> {
	public ListPlainPropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, Class<TI> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
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
	public Object filter(DMViewpoint viewpoint, K key, Object value) {
		if (itemFilter == null)
			return value;
		
		// We cheat here to filter a non-key value, because we never dereference
		Object result = itemFilter.filterKey(viewpoint, key, value);
		if (result == null)
			return Collections.emptyList();
		else
			return null;
	}

	@Override
	public void visitProperty(DMSession session, T object, FetchVisitor visitor) {
		for (Object value : (List<?>)getRawPropertyValue(object))
			visitor.plainProperty(this, value);
	}
}
