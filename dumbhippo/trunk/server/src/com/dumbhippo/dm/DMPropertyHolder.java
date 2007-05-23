package com.dumbhippo.dm;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import org.slf4j.Logger;

import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.identity20.Guid;

public class DMPropertyHolder implements Comparable<DMPropertyHolder> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DMPropertyHolder.class);
	
	private DMClassHolder<? extends DMObject> declaringClassHolder;
	private DMProperty annotation;
	private CtClass ctClass;
	private Class elementType;
	private boolean multiValued;
	private boolean resourceValued;
	private DMClassHolder<? extends DMObject> resourceClassHolder;
	private String methodName;
	private Method method;
	private String name;
	private String namespace;
	private String propertyId;
	private boolean defaultInclude;
	private Fetch defaultChildren;
	private long ordering;
	private boolean completed;
	
	public DMPropertyHolder (DMClassHolder<? extends DMObject> declaringClassHolder, CtMethod ctMethod, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		boolean booleanOnly = false;
		
		this.annotation = annotation;
		this.declaringClassHolder = declaringClassHolder;
		
		methodName = ctMethod.getName();
		
		if (methodName.startsWith("get") && methodName.length() >= 4) {
			name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
		} else if (methodName.startsWith("is") && methodName.length() >= 4) {
			name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
			booleanOnly = true;
		} else {
			throw new RuntimeException("DMProperty method name '" + ctMethod.getName() + "' doesn't look like a getter");
		}
		
		try {
			if (ctMethod.getParameterTypes().length > 0) {
				throw new RuntimeException("DMProperty method " + ctMethod.getName() + " has arguments");
			}

			ctClass = ctMethod.getReturnType();
			
			if (booleanOnly && ctClass != CtClass.booleanType)
				throw new RuntimeException("Getter starting with 'is' must have a boolean return");

			if (ctClass == CtClass.voidType)
				throw new RuntimeException("DMProperty method doesn't have a result");
			
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find bytecode for method return or parameters", e);
		}
		
		if (annotation.propertyId().equals(""))
			propertyId = declaringClassHolder.getClassId() + "#" + name;
		else
			propertyId = annotation.propertyId();
		
		// Validate the propertyId as an URI
		try {
			URI uri = new URI(propertyId);
			if (uri.getFragment() == null)
				throw new RuntimeException("propertyId '" + propertyId + "' must have a fragment identifier");
			
			// FIXME: Check that the fragment matches the name or implement code to manage
			//   the case. (How do you put two properties with the same localname on the
			//   same class if you require the method name to match the property name?)
			
		} catch (URISyntaxException e1) {
			throw new RuntimeException("propertyId '" + propertyId + "' is not a valid URI");
		}
		
		int hashIndex = propertyId.indexOf('#');
		namespace = propertyId.substring(0, hashIndex);
		
		computeOrdering();
		

		try {
			method = declaringClassHolder.getBaseClass().getMethod(methodName, new Class[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Can't find Java class object for method return type", e);
		}
		
		determineType(method);
	}
	
	public void complete() {
		if (completed)
			return;
		
		completed = true;
		
		if (resourceValued)
			resourceClassHolder = declaringClassHolder.getModel().getDMClass(getResourceType());

		defaultInclude = annotation.defaultInclude();
		if (!"".equals(annotation.defaultChildren())) {
			defaultInclude = true;
			
			try {
				FetchNode node = FetchParser.parse(annotation.defaultChildren());
				if (node.getProperties().length > 0) {
					if (!resourceValued)
						throw new RuntimeException(propertyId + ": non-empty defaultChildren can only be specified on a resource-valued property");
					
					defaultChildren = node.bind(resourceClassHolder);
				}
			} catch (RecognitionException e) {
				throw new RuntimeException(propertyId + ": failed to parse defaultChildren at char " + e.getColumn(), e);
			} catch (TokenStreamException e) {
				throw new RuntimeException(propertyId + ": failed to parse defaultChildren", e);
			}
		}
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
	
	public String getPropertyId() {
		return propertyId;
	}
	
	public String getNameSpace() {
		return namespace;
	}

	public boolean getDefaultInclude() {
		return defaultInclude;
	}
	
	public Fetch getDefaultChildren() {
		return defaultChildren;
	}
	
	public CtClass getCtClass() {
		return ctClass; 
	}
	
	public boolean isMultiValued() {
		return multiValued;
	}
	
	public boolean isResourceValued() {
		return resourceValued;
	}
	
	@SuppressWarnings("unchecked")
	public Class<? extends DMObject> getResourceType() {
		if (!resourceValued)
			throw new UnsupportedOperationException("Not a resource-valued property");
	
		return elementType;
	}
	
	public DMClassHolder<? extends DMObject> getResourceClassHolder() {
		if (!resourceValued)
			throw new UnsupportedOperationException("Not a resource-valued property");

		if (completed)
			return resourceClassHolder;
		else
			return declaringClassHolder.getModel().getDMClass(getResourceType());
	}
	
	public Class<?> getPlainType() {
		if (resourceValued)
			throw new UnsupportedOperationException("Not a plain-valued property");

		return elementType;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public Method getMethod() { 
		return method;
	}
	
	public String getName() {
		return name;
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

	public static DMPropertyHolder getForMethod(DMClassHolder<? extends DMObject> classHolder, CtMethod method) {
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
	
	// Having a quick global ordering for all properties allows us to easily
	// compute the intersection/difference of two sorted lists of properties,
	// something we need to do when fetching.
	//
	// We could do this by:
	//
	// a) Using java.lang.Object.hashCode(), but that's not 64-bit safe (though
	//    the chances of problems are miniscule.
	// b) Using the ordering of the propertyId string, but that's slow,
	//    especially since all propertyIds share a long common prefix.
	// c) Do a post-pass once all properties are registered to assign
	//    integer ordering. This would be very annoying since we use the 
	//    ordering when building DMClassHolder.
	//
	// Instead do:
	//
	// d) Compute 64-bits of a hash of the property ID and store it for later use
	//   
	// The main disadvantage of this compared to b) or c) is that the ordering
	// isn't predictable in advance or meaningful, though it should be stable
	// across server restart and even between different server instances.
	//
	private void computeOrdering() {
		MessageDigest messageDigest = Digest.newDigestMD5();
		byte[] bytes = messageDigest.digest(StringUtils.getBytes(propertyId));
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result = (result << 8) + bytes[i];
		}
		
		ordering = result;
	}

	public long getOrdering() {
		return ordering;
	}
	
	public int compareTo(DMPropertyHolder other) {
		return ordering < other.ordering ? -1 : (ordering == other.ordering ? 0 : 1);
	}
}
