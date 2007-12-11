package com.dumbhippo.dm.schema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import org.slf4j.Logger;

import com.dumbhippo.Digest;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMFeed;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.parser.FilterParser;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.dm.schema.PropertyInfo.ContainerType;
import com.dumbhippo.dm.store.StoreKey;

public abstract class DMPropertyHolder<K, T extends DMObject<K>, TI> implements Comparable<DMPropertyHolder<?,?,?>> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DMPropertyHolder.class);

	protected PropertyInfo<K,T,TI> propertyInfo;
	
	protected Class<TI> elementType;
	protected DMProperty annotation;
	
	protected boolean defaultInclude;
	private String typeString;
	protected String propertyId;
	protected Filter propertyFilter;
	protected boolean completed;
	
	private CtClass ctClass;
	private Method method;
	private String methodName;
	private String name;
	private String namespace;
	private long ordering;

	public DMPropertyHolder (PropertyInfo<K,T,TI> propertyInfo) {
		this.propertyInfo = propertyInfo;
		
		elementType = propertyInfo.getItemType();
		annotation = propertyInfo.getAnnotation();
		
		CtMethod ctMethod = propertyInfo.getCtMethod();
		
		boolean booleanOnly = false;
		
		try {
			method = propertyInfo.getDeclaringType().getMethod(ctMethod.getName(), new Class[] {});
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
		
		DMO classAnnotation = propertyInfo.getDeclaringType().getAnnotation(DMO.class);
		
		if (annotation.propertyId().equals(""))
			propertyId = classAnnotation.classId() + "#" + name;
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
		
		if (propertyInfo.getFilter() != null) {
			try {
				propertyFilter = FilterParser.parse(propertyInfo.getFilter().value());
			} catch (ParseException e) {
				throw new RuntimeException(propertyId + ": Error parsing filter", e);
			}
			
		}
	}
	
	public void complete() {
		if (completed)
			return;
		
		completed = true;
		
		defaultInclude = annotation.defaultInclude();
		initTypeString();
	}

	abstract protected PropertyType getType();

	private void initTypeString() {
		StringBuilder sb = new StringBuilder();
		
		if (defaultInclude)
			sb.append("+");
		
		sb.append(getType().getTypeChar());
		
		switch (getCardinality()) {
		case ZERO_ONE:
			sb.append("?");
			break;
		case ONE:
			break;
		case ANY:
			sb.append("*");
			break;
		}
		
		typeString = sb.toString(); 
	}
	
	public String getTypeString() {
		return typeString;
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
	
	public boolean isCached() {
		return annotation.cached();
	}

	public int getGroup() {
		return annotation.group();
	}
	
	public int getDefaultMaxFetch() {
		return annotation.defaultMaxFetch();
	}
	
	protected PropertyInfo<K,T,TI> getPropertyInfo() {
		return propertyInfo;
	}
	
	abstract public Object dehydrate(Object value);
	abstract public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session, boolean filter);
	abstract public Object filter(DMViewpoint viewpoint, K key, Object value);
	abstract public Cardinality getCardinality();
	
	public Object getRawPropertyValue(DMObject<?> object) {
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
	
	public DataModel getModel() {
		return propertyInfo.getModel();
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
	
	public int compareTo(DMPropertyHolder<?,?,?> other) {
		return ordering < other.ordering ? -1 : (ordering == other.ordering ? 0 : 1);
	}
	
	public abstract void visitChildren(DMSession session, BoundFetch<?,?> children, T object, FetchVisitor visitor);
	
	/**
	 * 
	 * @param session
	 * @param object
	 * @param visitor
	 * @param forceEmpty if True, call FetchVisitor.emptyProperty() on a missing property even if we
	 *    would normally omit it. This is used for a single-valued resource property, where we normally
	 *    don't emit any fetch result for missing properties, but must do so if we are notifying
	 *    that the property has gone away.
	 */
	public abstract void visitProperty(DMSession session, T object, FetchVisitor visitor, boolean forceEmpty);

	public abstract BoundFetch<?,?> getDefaultChildren();
	public abstract String getDefaultChildrenString();
	
	public Class<?> getKeyClass() {
		throw new UnsupportedOperationException();
	}
	
	@SuppressWarnings("unchecked")
	public <KI2,TI2 extends DMObject<KI2>> ResourcePropertyHolder<K,T,KI2,TI2> asResourcePropertyHolder(Class<KI2> keyClass) {
		return (ResourcePropertyHolder<K,T,KI2,TI2>)this;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	private static <K, T extends DMObject<K>, TI> PropertyInfo<K,T,TI> createPropertyInfo(Class<T> declaringType, Class<K> keyType, Class<TI> elementType) {
		return new PropertyInfo<K,T,TI>(declaringType, keyType, elementType);			
	}

	// The purpose of this is that Java (at least over the set of compilers we support) can't keep track
	// of the relationship between the parameters of DMClassInfo<>, and know that they are appropriate
	// to pass when creating ResourcePropertyInfo, so we have to do the construction unchecked.
	//
	@SuppressWarnings("unchecked")
	public static <K, T extends DMObject<K>> ResourcePropertyInfo<K,T,?,?> createResourcePropertyInfo(Class<T> declaringType, Class<K> keyType, DMClassInfo<?,?> elementClassInfo) {
		return new ResourcePropertyInfo(declaringType, keyType, elementClassInfo.getObjectClass(), elementClassInfo.getKeyClass());			
	}

	public static <K,T extends DMObject<K>> DMPropertyHolder<K,T,?> getForMethod(DataModel model, Class<T> declaringType, Class<K> keyType, CtMethod ctMethod) {
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
				throw new RuntimeException("@Viewpoint annotation must be on a @DMProperty");
			}
		
			return null;
		}

		Method method;
		try {
			method = declaringType.getMethod(ctMethod.getName(), new Class[] {});
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Can't find Java class object for method return type", e);
		}
		
		Type genericType = method.getGenericReturnType();
		Type genericElementType = genericType;

		ContainerType containerType = ContainerType.SINGLE;
		Class<?> elementType;
		
		// We handle StoreKey as a PlainType
		if (genericType instanceof ParameterizedType && method.getReturnType() != StoreKey.class) {
			ParameterizedType paramType = (ParameterizedType)genericType;
			Class<?> rawType = (Class<?>)paramType.getRawType();
			if (rawType == List.class)
				containerType = ContainerType.LIST;
			else if (rawType == Set.class)
				containerType = ContainerType.SET;
			else if (rawType == DMFeed.class)
				containerType = ContainerType.FEED;
			else
				throw new RuntimeException("List<?>, Set<?>, DMFeed<?> are the only currently supported parameterized types");
				
			if (paramType.getActualTypeArguments().length != 1)
				throw new RuntimeException("Couldn't understand type arguments to parameterized return type");
			
			genericElementType = paramType.getActualTypeArguments()[0];
		}
		
		if (genericElementType instanceof Class<?>)
			elementType = (Class<?>)genericElementType;
		else if (genericElementType instanceof ParameterizedType)
			elementType = (Class<?>)((ParameterizedType)genericElementType).getRawType();
		else
			throw new RuntimeException("Unexpected non-class type");
		
		DMClassInfo<?,? extends DMObject<?>> elementClassInfo = DMClassInfo.getForClass(elementType);
		
		PropertyInfo<K,T,?> propertyInfo;

		if (elementClassInfo != null) {
			propertyInfo = createResourcePropertyInfo(declaringType, keyType, elementClassInfo);
		} else if (elementType.isPrimitive() || (elementType == String.class) || (elementType == StoreKey.class) || (elementType == Date.class)) {
			propertyInfo = createPropertyInfo(declaringType, keyType, elementType);
		} else {
			throw new RuntimeException("Property type must be DMObject, primitive, Date, String, or StoreKey");
		}

		propertyInfo.setModel(model);
		propertyInfo.setCtMethod(ctMethod);
		propertyInfo.setAnnotation(property);
		propertyInfo.setFilter(filter);
		propertyInfo.setViewerDependent(viewerDependent);
		propertyInfo.setContainerType(containerType);
		
		return propertyInfo.createPropertyHolder();
	}
}
