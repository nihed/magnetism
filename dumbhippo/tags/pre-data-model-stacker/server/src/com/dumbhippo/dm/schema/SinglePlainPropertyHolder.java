package com.dumbhippo.dm.schema;

import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class SinglePlainPropertyHolder<K,T extends DMObject<K>, TI> extends PlainPropertyHolder<K,T,TI> {
	public SinglePlainPropertyHolder(PropertyInfo<K,T,TI> propertyInfo) {
		super(propertyInfo);
	}

	@Override
	public String getBoxPrefix() {
		if (elementType.isPrimitive()) {
			if (elementType == Boolean.TYPE)
				return "new Boolean(";
			else if (elementType == Byte.TYPE)
				return "new Byte(";
			else if (elementType == Character.TYPE)
				return "new Character(";
			else if (elementType == Short.TYPE)
				return "new Short(";
			else if (elementType == Integer.TYPE)
				return "new Integer(";
			else if (elementType == Long.TYPE)
				return "new Long(";
			else if (elementType == Float.TYPE)
				return "new Float(";
			else if (elementType == Double.TYPE)
				return "new Double(";
			else
				throw new RuntimeException("Unexpected primitive type");
		} else {
			return "";
		}
	}
	
	@Override
	public String getBoxSuffix() {
		if (elementType.isPrimitive()) {
			return ")";
		} else {
			return "";
		}
	}
	
	@Override
	public String getUnboxPrefix() {
		if (elementType.isPrimitive()) {
			if (elementType == Boolean.TYPE)
				return "((Boolean)";
			else if (elementType == Byte.TYPE)
				return "((Byte)";
			else if (elementType == Character.TYPE)
				return "((Character)";
			else if (elementType == Short.TYPE)
				return "((Short)";
			else if (elementType == Integer.TYPE)
				return "((Integer)";
			else if (elementType == Long.TYPE)
				return "((Long)";
			else if (elementType == Float.TYPE)
				return "((Float)";
			else if (elementType == Double.TYPE)
				return "((Double)";
			else
				throw new RuntimeException("Unexpected primitive type");
		} else {
			return super.getUnboxPrefix();
		}
	}
	
	@Override
	public String getUnboxSuffix() {
		if (elementType.isPrimitive()) {
			if (elementType == Boolean.TYPE)
				return ").booleanValue()";
			else if (elementType == Byte.TYPE)
				return ").byteValue()";
			else if (elementType == Character.TYPE)
				return ").charValue()";
			else if (elementType == Short.TYPE)
				return ").shortValue()";
			else if (elementType == Integer.TYPE)
				return ").intValue()";
			else if (elementType == Long.TYPE)
				return ").longValue()";
			else if (elementType == Float.TYPE)
				return ").floatValue()";
			else if (elementType == Double.TYPE)
				return ").doubleValue()";
			else
				throw new RuntimeException("Unexpected primitive type");
		} else {
			return super.getUnboxSuffix();
		}
	}
	
	@Override
	public Object filter(DMViewpoint viewpoint, K key, Object value) {
		if (itemFilter == null)
			return value;
		
		// We cheat here to filter a non-key value, because we never dereference
		return itemFilter.filterKey(viewpoint, key, value);
	}

	@Override
	public void visitProperty(DMSession session, T object, FetchVisitor visitor, boolean forceEmpty) {
		Object value = getRawPropertyValue(object);
		if (value != null)
			visitor.plainProperty(this, value);
		else if (forceEmpty)
			visitor.emptyProperty(this);
	}

	@Override
	public Cardinality getCardinality() {
		if (elementType.isPrimitive())
			return Cardinality.ONE;
		else
			return Cardinality.ZERO_ONE;
	}
}
