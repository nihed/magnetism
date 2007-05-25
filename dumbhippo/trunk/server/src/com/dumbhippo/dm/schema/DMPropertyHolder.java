package com.dumbhippo.dm.schema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
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
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.parser.FilterParser;

public abstract class DMPropertyHolder<K, T extends DMObject<K>, TI> implements Comparable<DMPropertyHolder> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DMPropertyHolder.class);
	
	protected DMClassHolder<K,T> declaringClassHolder;
	protected DMProperty annotation;
	protected boolean defaultInclude;
	protected String propertyId;
	protected Class<TI> elementType;
	protected Filter propertyFilter;
	protected boolean completed;
	
	private CtClass ctClass;
	private Method method;
	private String methodName;
	private String name;
	private String namespace;
	private long ordering;
	
	public DMPropertyHolder (DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, Class<TI> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		boolean booleanOnly = false;
		
		this.annotation = annotation;
		this.declaringClassHolder = declaringClassHolder;
		this.elementType = elementType;
		
		try {
			method = declaringClassHolder.getBaseClass().getMethod(ctMethod.getName(), new Class[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Can't find Java class object for method return type", e);
		}

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
		
		if (filter != null) {
			try {
				propertyFilter = FilterParser.parse(filter.value());
			} catch (RecognitionException e) {
				throw new RuntimeException(propertyId + ": Error parsing filter at " + e.line + ":" + e.column, e);
			} catch (TokenStreamException e) {
				throw new RuntimeException(propertyId + ": Error parsing filter", e);
			}
			
		}
	}
	
	public void complete() {
		if (completed)
			return;
		
		completed = true;
		
		defaultInclude = annotation.defaultInclude();
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
	
	public CtClass getCtClass() {
		return ctClass; 
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
		return "";
	}
	
	public String getBoxSuffix() {
		return "";
	}
	
	public String getUnboxPrefix() {
		return "(" + elementType.getName() + ")";
	}
	
	public String getUnboxSuffix() {
		return "";
	}
	
	abstract public Object dehydrate(Object value);
	abstract public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session);
	abstract public Object filter(DMViewpoint viewpoint, K key, Object value);
	
	public Object getRawPropertyValue(DMObject object) {
		try {
			return method.invoke(object);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		}
	}
	
	public static <K,T extends DMObject<K>> DMPropertyHolder<K,T,?> getForMethod(DMClassHolder<K,T> classHolder, CtMethod ctMethod) {
		DMProperty property = null;
		DMFilter filter = null;
		ViewerDependent viewerDependent = null;
		
		for (Object o : ctMethod.getAvailableAnnotations()) {
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

		Method method;
		try {
			method = classHolder.getBaseClass().getMethod(ctMethod.getName(), new Class[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Can't find Java class object for method return type", e);
		}
		
		Type genericType = method.getGenericReturnType();
		Type genericElementType = genericType;
		
		boolean multiValued = false;
		Class<?> elementType;
		
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
		
		DMClassInfo<?,?> classInfo = DMClassInfo.getForClass(elementType);

		if (classInfo != null) {
			return createResourcePropertyHolder(classHolder, ctMethod, classInfo, property, filter, viewerDependent, multiValued);
		} else if (elementType.isPrimitive() || (elementType == String.class)) { 
			return createPlainPropertyHolder(classHolder, ctMethod, elementType, property, filter, viewerDependent, multiValued);
		} else {
			throw new RuntimeException("Property type must be DMObject, primitive, or string");
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <K, T extends DMObject<K>> DMPropertyHolder<K,T,?> createResourcePropertyHolder(DMClassHolder<K,T> classHolder, CtMethod ctMethod, DMClassInfo<?,?> classInfo, DMProperty property, DMFilter filter, ViewerDependent viewerDependent, boolean multiValued) {
		if (multiValued)
			return new MultiResourcePropertyHolder(classHolder, ctMethod, classInfo, property, filter, viewerDependent);
		else
			return new SingleResourcePropertyHolder(classHolder, ctMethod, classInfo, property, filter, viewerDependent);
	}

	@SuppressWarnings("unchecked")
	private static <K, T extends DMObject<K>> DMPropertyHolder<K,T,?> createPlainPropertyHolder(DMClassHolder<K,T> classHolder, CtMethod ctMethod, Class<?> elementType, DMProperty property, DMFilter filter, ViewerDependent viewerDependent, boolean multiValued) {
		if (multiValued)
			return new MultiPlainPropertyHolder(classHolder, ctMethod, elementType, property, filter, viewerDependent);
		else
			return new SinglePlainPropertyHolder(classHolder, ctMethod, elementType, property, filter, viewerDependent);
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
	
	public abstract void visitChildren(DMSession session, Fetch<?,?> children, T object, FetchVisitor visitor);
	public abstract void visitProperty(DMSession session, T object, FetchVisitor visitor);

	public abstract Fetch<?,?> getDefaultChildren();
	public Class<?> getKeyClass() {
		throw new UnsupportedOperationException();
	}
	
	@SuppressWarnings("unchecked")
	public <KI2,TI2 extends DMObject<KI2>> ResourcePropertyHolder<K,T,KI2,TI2> asResourcePropertyHolder(Class<KI2> keyClass) {
		return (ResourcePropertyHolder<K,T,KI2,TI2>)this;
	}
}
