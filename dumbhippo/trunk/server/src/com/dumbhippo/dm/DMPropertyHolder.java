package com.dumbhippo.dm;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.identity20.Guid;

public class DMPropertyHolder {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DMPropertyHolder.class);
	
	private CtClass ctClass;
	private Class elementType;
	private boolean multiValued;
	private boolean resourceValued;
	private String methodName;
	private String name;
	private boolean defaultInclude;
	
	public DMPropertyHolder (DMClassHolder<?> classHolder, CtMethod method, DMProperty property, DMFilter filter, ViewerDependent viewerDependent) {
		boolean booleanOnly = false; 
		
		methodName = method.getName();
		
		if (methodName.startsWith("get") && methodName.length() >= 4) {
			name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
		} else if (methodName.startsWith("is") && methodName.length() >= 4) {
			name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
			booleanOnly = true;
		} else {
			throw new RuntimeException("DMProperty method name '" + method.getName() + "' doesn't look like a getter");
		}
		
		try {
			if (method.getParameterTypes().length > 0) {
				throw new RuntimeException("DMProperty method " + method.getName() + " has arguments");
			}

			ctClass = method.getReturnType();
			
			if (booleanOnly && ctClass != CtClass.booleanType)
				throw new RuntimeException("Getter starting with 'is' must have a boolean return");

			if (ctClass == CtClass.voidType)
				throw new RuntimeException("DMProperty method doesn't have a result");
			
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find bytecode for method return or parameters", e);
		}
		
		try {
			determineType(classHolder.getBaseClass().getMethod(methodName, new Class[] {}));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Can't find Java class object for method return type", e);
		}
		
		defaultInclude = property.defaultInclude();
	}

	private void determineType(Method method) {
		Type genericType = method.getGenericReturnType();
		Type genericElementType = genericType;
		
		if (genericType instanceof ParameterizedType) {
			ParameterizedType paramType = (ParameterizedType)genericType;
			Class<?> rawType = (Class<?>)paramType.getRawType();
			if (Collection.class.isAssignableFrom(rawType)) {
				if (rawType != List.class)
					throw new RuntimeException("List<?> is the only currently supported parameterized type");
				
				multiValued = true;
				if (paramType.getActualTypeArguments().length != 1)
					throw new RuntimeException("Couldn't understand type arguments to parameterized return type");
				
				genericElementType = paramType.getActualTypeArguments()[0];
			}
		}
		
		if (genericElementType instanceof Class<?>)
			elementType = (Class<?>)genericElementType;
		else if (genericElementType instanceof ParameterizedType)
			elementType = (Class<?>)((ParameterizedType)genericElementType).getRawType();
		else
			throw new RuntimeException("Unexpected non-class type");
		
		if (elementType.isPrimitive())
			return;
		else if (elementType == String.class)
			return;
		else if (DMObject.class.isAssignableFrom(elementType)) {
			resourceValued = true;
			return;
		} else
			throw new RuntimeException("Property type must be primitive, string");
	}
	
	public CtClass getCtClass() {
		return ctClass; 
	}
	
	public boolean isMultiValued() {
		return multiValued;
	}
	
	public Class getElementType() {
		return elementType;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean getDefaultInclude() {
		return defaultInclude;
	}
	
	public String getBoxPrefix() {
		if (!multiValued && elementType.isPrimitive()) {
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
	
	public String getBoxSuffix() {
		if (!multiValued && elementType.isPrimitive()) {
			return ")";
		} else {
			return "";
		}
	}
	
	public String getUnboxPrefix() {
		if (!multiValued && elementType.isPrimitive()) {
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
		} else if (multiValued) {
			return "(java.util.List)";
		} else {
			return "(" + elementType.getName() + ")";
		}
	}
	
	public String getUnboxSuffix() {
		if (!multiValued && elementType.isPrimitive()) {
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
			return "";
		}
	}
	
	private Object dehydrateDMO(Object value) {
		Object key = ((DMObject)value).getKey();
		if ((key instanceof Guid) || (key instanceof String)) {
			return key;
		} else {
			return ((DMKey)key).clone();
		}
	}
	
	public Object dehydrate(Object value) {
		if (multiValued && resourceValued) {
			List<Object> result = new ArrayList<Object>();
			for (Object o : (Collection<?>)value)
				result.add(dehydrateDMO(o));
			
			return result;
		} else if (resourceValued) {
			return dehydrateDMO(value);
		} else {
			return value;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Object rehydrateDMO(Object value, DMSession session) {
		try {
			return session.find(elementType, value);
		} catch (com.dumbhippo.server.NotFoundException e) {
			// FIXME: find() basically always has to exceed, because of lazy initialization
			throw new RuntimeException("Unexpectedly could not find object when rehydrating");
		}
	}
	
	public Object rehydrate(Object value, DMSession session) {
		// FIXME: we probably want to filter at the same time to avoid
		// instantiation of unnecesary objects
		
		if (multiValued && resourceValued) {
			List<Object> result = new ArrayList<Object>();
			for (Object o : (Collection<?>)value) {
				result.add(rehydrateDMO(o, session));
			}
			
			return result;
		} else if (resourceValued) {
			return rehydrateDMO(value, session);
		} else {
			return value;
		}
	}

	public static DMPropertyHolder getForMethod(DMClassHolder<?> classHolder, CtMethod method) {
		DMProperty property = null;
		DMFilter filter = null;
		ViewerDependent viewerDependent = null;
		
		for (Object o : method.getAvailableAnnotations()) {
			if (o instanceof DMProperty) {
				property = (DMProperty)o;
			} else if (o instanceof DMFilter) {
				filter = (DMFilter)o;
			} else if (o instanceof ViewerDependent) 
				viewerDependent = (ViewerDependent)o;
		}
		
		if (property == null) {
			if (filter != null) {
				throw new RuntimeException("@DMFilter annotation must be on a @DMProperty");
			}
			if (viewerDependent != null) {
				throw new RuntimeException("@DMFilter annotation must be on a @DMProperty");
			}
		
			return null;
		}
		
		return new DMPropertyHolder(classHolder, method, property, filter, viewerDependent);
	}
}
