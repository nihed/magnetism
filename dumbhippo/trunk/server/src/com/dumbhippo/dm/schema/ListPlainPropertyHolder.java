package com.dumbhippo.dm.schema;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class ListPlainPropertyHolder<K, T extends DMObject<K>, TI>  extends PlainPropertyHolder<K,T,TI> {
	public ListPlainPropertyHolder(PropertyInfo<K,T,TI> propertyInfo) {
		super(propertyInfo);
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
			return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitProperty(DMSession session, T object, FetchVisitor visitor, boolean forceEmpty) {
		visitPlainValues(session, (List<TI>)getRawPropertyValue(object), visitor);
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ANY;
	}
}
