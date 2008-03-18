package com.dumbhippo.dm.schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.ChangeNotification;
import com.dumbhippo.dm.ClientMatcher;
import com.dumbhippo.dm.DMInjectionLookup;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMInit;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.MetaConstruct;
import com.dumbhippo.dm.filter.CompiledFilter;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.CompiledListFilter;
import com.dumbhippo.dm.filter.CompiledSetFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;
import com.dumbhippo.dm.parser.FilterParser;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.util.EJBUtil;

public class DMClassHolder<K,T extends DMObject<K>> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DMClassHolder.class);
	
	private DataModel model;
	private Class<T> dmoClass;
	DMClassHolder<K,? extends DMObject<K>> baseClassHolder;
	private List<DMClassHolder<?, ?>> derivedClasses = new ArrayList<DMClassHolder<?,?>>();
	private Class<K> keyClass;
	private Constructor<K> keyStringConstructor;
	private Method metaConstructor;
	private Constructor<? extends T> wrapperConstructor;
	private DMPropertyHolder<K,T,?>[] properties;
	private boolean[] mustQualify;

	// Groups declared in this class (not parent classes)
	private Map<Integer, PropertyGroup> groups = new HashMap<Integer, PropertyGroup>();
	// Copies of parent groups (with the level set to the depth of the inheritance)
	private List<PropertyGroup> parentGroups = new ArrayList<PropertyGroup>();
	// Map from all properties relevant to this class (properties of this class and parent classes) 
	// to the group for the property
	private Map<DMPropertyHolder, PropertyGroup> groupsByProperty = new HashMap<DMPropertyHolder, PropertyGroup>();

	private Map<String, Integer> propertiesMap = new HashMap<String, Integer>();
	private Map<String, Integer> feedPropertiesMap = new HashMap<String, Integer>();
	private DMO annotation;

	private Filter filter;
	private Filter itemFilter;
	private CompiledFilter<K,T> compiledFilter;
	private CompiledItemFilter<?,?,K,T> compiledItemFilter;
	private CompiledListFilter<?,?,K,T> compiledListFilter;
	private CompiledSetFilter<?,?,K,T> compiledSetFilter;

	public DMClassHolder(DataModel model, DMClassInfo<K,T> classInfo) {
		this.model = model;
		dmoClass = classInfo.getObjectClass();
		keyClass = classInfo.getKeyClass();
		
		if (keyClass != Guid.class && keyClass != String.class && keyClass != Long.class) {
			try {
				keyStringConstructor = keyClass.getConstructor(String.class);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(dmoClass.getName() + ": Can't find constructor for key class from a string");
			}
			
			for (Class<?> exceptionClass : keyStringConstructor.getExceptionTypes()) {
				if (!(RuntimeException.class.isAssignableFrom(exceptionClass) ||
					  BadIdException.class.isAssignableFrom(exceptionClass)))
					throw new RuntimeException(dmoClass.getName() + ": String key constructor can only throw BadIdException");
			}
		}
		
		annotation = dmoClass.getAnnotation(DMO.class);
		if (annotation == null)
			throw new RuntimeException("DMObject class " + dmoClass.getName() + " doesn't have a @DMO annotation");
		
		// Validate the classId as an URI
		try {
			URI uri = new URI(annotation.classId());
			if (uri.getFragment() != null)
				throw new RuntimeException(dmoClass.getName() + ": classId '" + annotation.classId() + "' can't have a fragment identifier");

		} catch (URISyntaxException e1) {
			throw new RuntimeException(dmoClass.getName() + ": classId '" + annotation.classId() + "' is not a valid URI");
		}
		
		Class<?> parentClass = dmoClass.getSuperclass();
		while (parentClass != null) {
			if (parentClass.isAnnotationPresent(DMO.class)) {
				DMClassHolder<?,?> classHolder = model.getClassHolder(parentClass);
				if (classHolder.getKeyClass() != keyClass) {
					throw new RuntimeException(dmoClass.getName() + ": parent class " + parentClass.getName() + " has a different key type");
				}
				@SuppressWarnings("unchecked")
				DMClassHolder<K,T> tmpHolder = (DMClassHolder<K,T>)classHolder;
				tmpHolder.derivedClasses.add(this);
				baseClassHolder = tmpHolder;
			}
			
			parentClass = parentClass.getSuperclass();
		}
		
		if ("".equals(annotation.resourceBase())) {
			if (baseClassHolder == null)
				throw new RuntimeException(dmoClass.getName() + ": resourceBase must be specified for base class in DMO inheritance heirarchy");
		} else {
			if (baseClassHolder != null)
				throw new RuntimeException(dmoClass.getName() + ": resourceBase must not be specified for derived DMO class");
		}
		
		for (Method method : dmoClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(MetaConstruct.class)) {
				if (metaConstructor != null)
					throw new RuntimeException(dmoClass.getName() + ": Two @MetaConstruct method");
				if (!(Class.class.isAssignableFrom(method.getReturnType())))
					throw new RuntimeException(dmoClass.getName() + ": @MetaConstruct method must return a class");
				if (method.getParameterTypes().length != 1)
					throw new RuntimeException(dmoClass.getName() + ": @MetaConstruct method must take a single parameter");
				if (!method.getParameterTypes()[0].isAssignableFrom(keyClass))
					throw new RuntimeException(dmoClass.getName() + ": @MetaConstruct method parameter must match key type");
				
				metaConstructor = method;
			}
		}

		buildWrapperClass();
		
		DMFilter filterAnnotation = dmoClass.getAnnotation(DMFilter.class);
		if (filterAnnotation != null) {
			if (baseClassHolder != null) 
				throw new RuntimeException(dmoClass.getName() + ": @DMFilter annotation must be specified on the base class");
			
			try {
				filter = FilterParser.parse(filterAnnotation.value());
			} catch (com.dumbhippo.dm.parser.ParseException e) {
				throw new RuntimeException(dmoClass.getName() + ": Error parsing filter", e);
			}
			
			itemFilter = filter.asItemFilter();
		
			compiledFilter = FilterCompiler.compileFilter(model, keyClass, filter);
			CompiledItemFilter<Object,DMObject<Object>,K,T> genericItemFilter = FilterCompiler.compileItemFilter(model, Object.class, keyClass, itemFilter);
			compiledItemFilter = genericItemFilter;
			CompiledListFilter<Object,DMObject<Object>,K,T> genericListFilter = FilterCompiler.compileListFilter(model, Object.class, keyClass, itemFilter);
			compiledListFilter = genericListFilter;
			CompiledSetFilter<Object,DMObject<Object>,K,T> genericSetFilter = FilterCompiler.compileSetFilter(model, Object.class, keyClass, itemFilter);
			compiledSetFilter = genericSetFilter;
		}
	}
	
	public void complete() {
		for (DMPropertyHolder<?,?,?> property : properties)
			property.complete();
	}

	public DataModel getModel() {
		return model;
	}
	
	public String getResourceBase() {
		if (baseClassHolder != null)
			return baseClassHolder.getResourceBase();
		else
			return annotation.resourceBase();
	}
	
	public String getClassId() {
		return annotation.classId();
	}

	public Class<K> getKeyClass() {
		return keyClass;
	}
	
	public Class<T> getDMOClass() {
		return dmoClass;
	}
	
	/**
	 * @return a list of classholders for all DMO classes derived directly or indirectly
	 *   from this DMO class. 
	 */
	public List<DMClassHolder<?,?>> getDerivedClasses() {
		return derivedClasses;
	}
	
	public int getPropertyIndex(String name) {
		Integer index = propertiesMap.get(name);
		if (index == null)
			return -1;
		
		return index;
	}
	
	/**
	 * Get the index of the property with the specified name among feed-valued
	 * properties of the class. Can be used along with getFeedPropertiesCount()
	 * to store feed-specific data for a instance of this class in an array.
	 */
	public int getFeedPropertyIndex(String name) {
		Integer index = feedPropertiesMap.get(name);
		if (index == null)
			return -1;
		
		return index;
	}
	
	/**
	 * see getFeedPropertyIndex(). 
	 */
	public int getFeedPropertiesCount() {
		return feedPropertiesMap.size();
	}
	
	public int getPropertyCount() {
		return properties.length;
	}
	
	public DMPropertyHolder<K,T,?> getProperty(int propertyIndex) {
		return properties[propertyIndex];
	}
	
	public DMPropertyHolder<K,T,?>[] getProperties() {
		return properties;
	}
	
	public boolean mustQualifyProperty(int propertyIndex) {
		return mustQualify[propertyIndex];
	}
	
	public T createInstance(K key, DMSession session) {
		try {
			return wrapperConstructor.newInstance(key, session);
		} catch (Exception e) {
			throw new RuntimeException("Error creating instance of class " + dmoClass.getName(), e);
		}
	}
	
	public Filter getUncompiledFilter() {
		return filter;
	}
	
	public Filter getUncompiledItemFilter() {
		return itemFilter;
	}
	
	public CompiledFilter<K,T> getFilter() {
		return compiledFilter;
	}

	@SuppressWarnings("unchecked")
	public <KO, TO extends DMObject<KO>> CompiledItemFilter<KO,TO,K,T> getItemFilter() {
		return (CompiledItemFilter<KO,TO,K,T>)compiledItemFilter;
	}
	
	@SuppressWarnings("unchecked")
	public <KO, TO extends DMObject<KO>> CompiledListFilter<KO,TO,K,T> getListFilter() {
		return (CompiledListFilter<KO,TO,K,T>)compiledListFilter;
	}

	@SuppressWarnings("unchecked")
	public <KO, TO extends DMObject<KO>>  CompiledSetFilter<KO,TO,K,T> getSetFilter() {
		return (CompiledSetFilter<KO,TO,K,T>)compiledSetFilter;
	}

	public String makeResourceId(K key) {
		return model.getBaseUrl() + getResourceBase() + "/" + key.toString();
	}
	
	public String makeRelativeId(K key) {
		return getResourceBase() + "/" + key.toString();
	}

	@SuppressWarnings("unchecked")
	private DMClassHolder<K,? extends T> getClassHolderForKey(K key) {
		if (metaConstructor != null) {
			try {
				Class<?> clazz = (Class<?>)metaConstructor.invoke(null, new Object[] { key });
				return (DMClassHolder<K,? extends T>)model.getClassHolder(clazz);
			} catch (Exception e) {
				throw new RuntimeException("Error calling metaconstructor of class " + dmoClass.getName(), e);
			}
		} else {
			return this;
		}
	}

	@SuppressWarnings("unchecked")
	public StoreKey<K,? extends T> makeStoreKey(K key) {
		DMClassHolder subClassHolder = getClassHolderForKey(key);
		return new StoreKey(subClassHolder, key);
	}
	
	@SuppressWarnings("unchecked")
	public ChangeNotification<K,? extends T> makeChangeNotification(K key, ClientMatcher matcher) {
		DMClassHolder subClassHolder = getClassHolderForKey(key);
		return new ChangeNotification(subClassHolder.getDMOClass(), key, matcher);
	}
	
	public StoreKey<K,? extends T> makeStoreKey(String string) throws BadIdException {
		
		if (keyClass == Guid.class) {
			try {
				Guid guid = new Guid(string);
				
				@SuppressWarnings("unchecked")
				StoreKey<K,? extends T> key = makeStoreKey((K)guid);
				return key;
			} catch (ParseException e) {
				throw new BadIdException("Invalid GUID in resourceId");
			}
			
		} else if (keyClass == String.class) {
			@SuppressWarnings("unchecked")
			StoreKey<K,? extends T> key = makeStoreKey((K)string);
			return key;
		} else if (keyClass == Long.class) {
			try {
				Long l = Long.parseLong(string);
				
				@SuppressWarnings("unchecked")
				StoreKey<K,? extends T> key = makeStoreKey((K)l);
				return key;
			} catch (NumberFormatException e) {
				throw new BadIdException("Invalid long in resourceId");
			}
		} else {
			try {
				K keyObject = keyStringConstructor.newInstance(string);
				
				return makeStoreKey(keyObject);
			} catch (InstantiationException e) {
				throw new RuntimeException("Error creating key object from string", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Error creating key object from string", e);
			} catch (InvocationTargetException e) {
				Throwable targetException = e.getTargetException();
				if (targetException instanceof BadIdException)
					throw (BadIdException)targetException;
				else
					throw new RuntimeException("Unexpected exception creating key object from string", e);
			}
		}
	}

	public void processInjections(DMSession session, T t) {
		DMInjectionLookup injectionLookup = model.getInjectionLookup();
		
		Class<?> clazz = dmoClass;
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				Object injection = null;
				
				if (injectionLookup != null)
					injection = injectionLookup.getInjectionByAnnotations(field.getType(), field.getAnnotations(), session);

				if (injection == null && field.isAnnotationPresent(Inject.class)) {
					if (injectionLookup != null)
						injection = injectionLookup.getInjectionByType(field.getType(), session);
					if (injection == null)
						injection = getInjectionByType(field.getType(), session);
				}
				
				if (injection != null)
					setField(t, field, injection);			
				
				if (field.isAnnotationPresent(EJB.class)) {
					Object bean = EJBUtil.defaultLookup(field.getType());
					setField(t, field, bean);			
				}
			}
			
			clazz = clazz.getSuperclass();
		}
	}
	
	private Object getInjectionByType(Class<?> type, DMSession session) {
		if (type == EntityManager.class) {
			return session.getInjectableEntityManager();
		} else if (type == DMSession.class) {
			return session;
		} else if (DMViewpoint.class.isAssignableFrom(type)) {
			// We use a isAssignableFrom check here to allow people to @Inject fields
			// that are subclasses of DMViewpoint. If the type of the @Inject field
			// is a subtype of DMViewpoint not compatible with the the viewpoint of
			// the DMSession, then this we'll get a ClassCastException here
			return session.getViewpoint();
		} else { 
			throw new RuntimeException("@Inject annotation found field of unknown type " + type.getName());
		}
	}

	private void setField(T t, Field field, Object value) {
		try {
			// Like EJB3, we support private-field injection
			field.setAccessible(true);
			field.set(t, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error injecting object", e);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////

	static final Pattern TEMPLATE_PARAMETER = Pattern.compile("%([A-Za-z_][A-Za-z0-9_]+)%");

	// Simple string-template facility for the generated methods
	private static class Template {
		private String template;
		private Map<String, String> parameters = new HashMap<String, String>();
		
		public Template(String template) {
			this.template = template;
		}
		
		public void setParameter(String name, String value) {
			parameters.put(name, value);
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			Matcher m = TEMPLATE_PARAMETER.matcher(template);
			while (m.find()) {
				String replacement = parameters.get(m.group(1));
				if (replacement == null)
					throw new RuntimeException("No replacement for template parameter '" + m.group(1) + "'");
			    m.appendReplacement(sb, replacement);
			}
			 m.appendTail(sb);
			 return sb.toString();
		}
	}
	
	private CtClass ctClassForClass(Class<?> c) {
		ClassPool classPool = model.getClassPool();
		String className = c.getName();

		try {
			return classPool.get(className);
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find the bytecode for" + className);
		}
	}
	
	private PropertyGroup ensureGroup(int index) {
		PropertyGroup group = groups.get(index);
		if (!groups.containsKey(index)) {
			group = new PropertyGroup(index);
			groups.put(index, group);
		}
		
		return group;
	}
	
	private void collateProperties(CtClass baseCtClass) {
		List<DMPropertyHolder<K,T,?>> foundProperties = new ArrayList<DMPropertyHolder<K,T,?>>();
		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		
		for (CtMethod method : baseCtClass.getDeclaredMethods()) {
			DMPropertyHolder<K,T,?> property = DMPropertyHolder.getForMethod(model, dmoClass, keyClass, method);
			if (property != null) {
				foundProperties.add(property);
				if (!nameCount.containsKey(property.getName()))
					nameCount.put(property.getName(), 1);
				else
					nameCount.put(property.getName(), 1 + nameCount.get(property.getName()));
				
				if (property.getGroup() >= 0) {
					PropertyGroup group = ensureGroup(property.getGroup());
					group.addProperty(property);
					groupsByProperty.put(property, group);
				}
			} else {
				Object[] annotations;
				
				try {
					annotations = method.getAnnotations();
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(dmoClass.getName() + ": Problem looking up annotations", e);
				}
				
				for (Object annotation : annotations) {
					if (annotation instanceof DMInit) {
						DMInit init = (DMInit)annotation;
						ensureGroup(init.group()).setInitMethod(method, init);
					}
				}
			}
		}
		
		Class<?> parentClass = dmoClass.getSuperclass();
		int level = 1;
		while (parentClass != null) {
			DMO parentAnnotation = parentClass.getAnnotation(DMO.class);
			if (parentAnnotation != null) {
				DMClassHolder<?,?> parentClassHolder = model.getClassHolder(parentClass);
				
				for (int i = 0; i < parentClassHolder.getPropertyCount(); i++) {
					@SuppressWarnings("unchecked")
					DMPropertyHolder<K,T,?> property = (DMPropertyHolder<K,T,?>)parentClassHolder.getProperty(i);
					
					// Only get properties declared in the parent class, we'll pick up inherited
					// properties as we walk further up the inheritance chain
					if (property.getPropertyInfo().getDeclaringType() == parentClass) {
						foundProperties.add(property);
						if (!nameCount.containsKey(property.getName()))
							nameCount.put(property.getName(), 1);
						else
							nameCount.put(property.getName(), 1 + nameCount.get(property.getName()));
					}
				}
				
				for (Object o : parentClassHolder.groups.values()) {
					PropertyGroup group = (PropertyGroup)o;
					PropertyGroup groupCopy = new PropertyGroup(group, level); 
					parentGroups.add(groupCopy);
					for (DMPropertyHolder<K,T,?> property : group.getProperties())
						groupsByProperty.put(property, groupCopy);
				}
			}
			
			parentClass = parentClass.getSuperclass();
			level++;
		}
		
		// Sort the properties based on the ordering we impose on DMPropertyHolder 
		// (see comment for DMPropertyHolder.computeHash()
		Collections.sort(foundProperties);
		
		@SuppressWarnings("unchecked")
		DMPropertyHolder<K,T,?>[] tmpProperties = foundProperties.toArray(new DMPropertyHolder[foundProperties.size()]);
		
		properties = tmpProperties;
		mustQualify = new boolean[foundProperties.size()];

		int feedPropertiesCount = 0;
		for (int i = 0; i < properties.length; i++) {
			DMPropertyHolder<?,?,?> property = properties[i];
			
			propertiesMap.put(property.getName(), i);
			if (property instanceof FeedPropertyHolder)
				feedPropertiesMap.put(property.getName(), feedPropertiesCount++);
			mustQualify[i] = nameCount.get(property.getName()) > 1;
		}
	}

	private void addCommonFields(CtClass wrapperCtClass) throws CannotCompileException {
		CtField field;
		
		CtClass dmSessionCtClass = ctClassForClass(DMSession.class);
		
		field = new CtField(dmSessionCtClass, "_dm_session", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
		
		field = new CtField(CtClass.booleanType, "_dm_injected", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);

		field = new CtField(CtClass.booleanType, "_dm_initialized", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
	}
	
	private void addConstructor(CtClass wrapperCtClass) throws CannotCompileException {
		CtClass dmSessionCtClass = ctClassForClass(DMSession.class);
		CtClass keyCtClass = ctClassForClass(keyClass);
		
		CtConstructor constructor = new CtConstructor(new CtClass[] { keyCtClass, dmSessionCtClass }, wrapperCtClass);
		
		constructor.setBody("{ super($1); _dm_session = $2;}");
		
		wrapperCtClass.addConstructor(constructor);
	}
	
	private void addInitMethod(CtClass baseCtClass, CtClass wrapperCtClass) throws CannotCompileException {
		CtMethod method = new CtMethod(CtClass.voidType, "_dm_init", new CtClass[] {}, wrapperCtClass);
		Template body = new Template(
			"{" +
			"  if (!_dm_initialized) {" +
			"      if (!_dm_injected) {" +
			"          _dm_classHolder.processInjections(_dm_session, this);" +
			"          _dm_injected = true;" +
			"      }" +
			"      try {" +
			"          init();" +
			"      } catch (com.dumbhippo.server.NotFoundException e) {" +
			"          throw new com.dumbhippo.dm.LazyInitializationException(e);" +
			"      }" +
			"      _dm_initialized = true;" +
			"    }" +
			"}");
		method.setBody(body.toString());
		wrapperCtClass.addMethod(method);
		
		method = new CtMethod(CtClass.booleanType, "isInitialized", new CtClass[] {}, wrapperCtClass);
		method.setBody(
		   "{" +
		   "    return _dm_initialized;" +
		   "}");
		wrapperCtClass.addMethod(method);
	}
	
	private void addGroupInitMethod(CtClass wrapperCtClass, PropertyGroup group) throws CannotCompileException {
		String groupSuffix = group.getLevel() + "_" + group.getIndex();
		
		CtField field = new CtField(CtClass.booleanType, "_dm_initializedGroup" + groupSuffix, wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
		
		CtMethod method = new CtMethod(CtClass.voidType, "_dm_initGroup" + groupSuffix, new CtClass[] {}, wrapperCtClass);
		StringBuilder body = new StringBuilder();
		
		body.append("{" +
					"   if (_dm_initializedGroup" + groupSuffix + ")" +
					"       return;");
		
		if (group.getInitMain())
			body.append(
					"   _dm_init();");
		else
			body.append(
					"  if (!_dm_injected) {" +
					"     _dm_classHolder.processInjections(_dm_session, this);" +
					"     _dm_injected = true;" +
					"  }");
			
		if (group.getInitMethod() != null) {
			body.append(
					"  try {" +
					"     " + group.getInitMethod() + "();" +
					"  } catch (com.dumbhippo.server.NotFoundException e) {" +
					"      throw new com.dumbhippo.dm.LazyInitializationException(e);" +
					"  }");
		}
		
		for (DMPropertyHolder<K,T,?> property : group.getProperties()) {
			Template propertyInit = new Template(
					"  _dm_%propertyName% = %unboxPre%_dm_session.storeAndFilter(getStoreKey(), %propertyIndex%, %boxPre%super.%methodName%()%boxPost%)%unboxPost%;" +
					"  _dm_%propertyName%Initialized = true;");
			
			propertyInit.setParameter("propertyName", property.getName());
			propertyInit.setParameter("boxPre", property.getBoxPrefix());
			propertyInit.setParameter("boxPost", property.getBoxSuffix());
			propertyInit.setParameter("unboxPre", property.getUnboxPrefix());
			propertyInit.setParameter("unboxPost", property.getUnboxSuffix());
			propertyInit.setParameter("propertyIndex", Integer.toString(getPropertyIndex(property.getName())));
			propertyInit.setParameter("methodName", property.getMethodName());

			body.append(propertyInit.toString());
		}
		
		body.append(
				"   _dm_initializedGroup" + groupSuffix + " = true;" +
				"}");
		
		method.setBody(body.toString());
		wrapperCtClass.addMethod(method);
	}
	
	private void addGroupInitMethods(CtClass wrapperCtClass) throws CannotCompileException {
		for  (PropertyGroup group : groups.values()) 
			addGroupInitMethod(wrapperCtClass, group);
		
		for  (PropertyGroup group : parentGroups) 
			addGroupInitMethod(wrapperCtClass, group);
	}

	private void addGetClassHolderMethod(CtClass wrapperCtClass) throws CannotCompileException {
		CtClass dmClassHolderCtClass = ctClassForClass(DMClassHolder.class);

		CtField field = new CtField(dmClassHolderCtClass, "_dm_classHolder", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE | Modifier.STATIC);
		wrapperCtClass.addField(field);

		CtMethod method = new CtMethod(dmClassHolderCtClass, "getClassHolder", new CtClass[] {}, wrapperCtClass);
		method.setBody(
			"{" +
			"    return _dm_classHolder;" +
			"}");
		
		wrapperCtClass.addMethod(method);
	}

	private void addFeedWrapperGetter(CtClass wrapperCtClass, DMPropertyHolder<?,?,?> property, int propertyIndex) throws CannotCompileException {
		CtMethod wrapperMethod = new CtMethod(property.getCtClass(), property.getMethodName(), new CtClass[] {}, wrapperCtClass);

		if (property.getGroup() >= 0)
			throw new RuntimeException("Feed property '" + property.getName() + "' cannot be grouped.");
		
		Template body = new Template(
				"{" +
				"    if (!_dm_%propertyName%Initialized) {" +
				"        _dm_init();" +
				"        _dm_%propertyName% = _dm_session.createFeedWrapper(getStoreKey(), %propertyIndex%, super.%methodName%());" +
				"        _dm_%propertyName%Initialized = true;" +
				"    }" +
				"    return _dm_%propertyName%;" +
				"}");

		body.setParameter("methodName", property.getMethodName());
		body.setParameter("propertyName", property.getName());
		body.setParameter("propertyIndex", Integer.toString(propertyIndex));
		wrapperMethod.setBody(body.toString());
		
		wrapperCtClass.addMethod(wrapperMethod);
	}
	
	private void addWrapperGetter(CtClass wrapperCtClass, DMPropertyHolder<?,?,?> property, int propertyIndex) throws CannotCompileException, NotFoundException {
		CtMethod wrapperMethod = new CtMethod(property.getCtClass(), property.getMethodName(), new CtClass[] {}, wrapperCtClass);
		
		String storeCommands;
		PropertyGroup group = groupsByProperty.get(property); 
		
		if (group == null) {
			storeCommands =
				"_dm_init();" +
				"_dm_%propertyName% = %unboxPre%_dm_session.storeAndFilter(getStoreKey(), %propertyIndex%, %boxPre%super.%methodName%()%boxPost%)%unboxPost%;" +
				"_dm_%propertyName%Initialized = true;";
		} else {
			storeCommands = 
				"_dm_initGroup%groupSuffix%();";
		}
		
		Template body = new Template(
			"{" +
			"    if (!_dm_%propertyName%Initialized) {" +
			"    	 try {" +
			"           _dm_%propertyName% = %unboxPre%_dm_session.fetchAndFilter(getStoreKey(), %propertyIndex%)%unboxPost%;" +
			"           _dm_%propertyName%Initialized = true;" +
			"    	 } catch (com.dumbhippo.dm.NotCachedException e) {" +
			storeCommands +
			"        }" +
			"    }" +
			"    return _dm_%propertyName%;" +
			"}");

		body.setParameter("propertyName", property.getName());
		body.setParameter("boxPre", property.getBoxPrefix());
		body.setParameter("boxPost", property.getBoxSuffix());
		body.setParameter("unboxPre", property.getUnboxPrefix());
		body.setParameter("unboxPost", property.getUnboxSuffix());
		body.setParameter("propertyIndex", Integer.toString(propertyIndex));
		body.setParameter("groupSuffix", group != null ? (group.getLevel() + "_" + group.getIndex()) : "~~~");
		body.setParameter("methodName", property.getMethodName());
		wrapperMethod.setBody(body.toString());
		
		wrapperCtClass.addMethod(wrapperMethod);
	}

	private void addPropertyFields(CtClass wrapperCtClass) throws CannotCompileException, NotFoundException {
		for (int i = 0; i < properties.length; i++) {
			DMPropertyHolder<?,?,?> property = properties[i];
			
			CtField field;
			
			field = new CtField(CtClass.booleanType, "_dm_" + property.getName() + "Initialized", wrapperCtClass);
			field.setModifiers(Modifier.PRIVATE);
			wrapperCtClass.addField(field);

			field = new CtField(property.getCtClass(), "_dm_" + property.getName(), wrapperCtClass);
			field.setModifiers(Modifier.PRIVATE);
			wrapperCtClass.addField(field);
		}
	}
	
	private void addWrapperGetters(CtClass wrapperCtClass) throws CannotCompileException, NotFoundException {
		for (int i = 0; i < properties.length; i++) {
			DMPropertyHolder<?,?,?> property = properties[i];
			
			if (property instanceof FeedPropertyHolder)
				addFeedWrapperGetter(wrapperCtClass, property, i);
			else
				addWrapperGetter(wrapperCtClass, property, i);
		}
	}
	
	private void injectClassHolder(Class<?> wrapperClass) {
		try {
			Field classHolderField = wrapperClass.getDeclaredField("_dm_classHolder");
			classHolderField.setAccessible(true);
			classHolderField.set(null, this);
			classHolderField.setAccessible(false);
		} catch (Exception e) {
			throw new RuntimeException("Error injecting DMClassHolder into wrapper class", e);
		}
	}
	
	private void buildWrapperClass() {
		String className = dmoClass.getName();
		ClassPool classPool = model.getClassPool();
		CtClass baseCtClass = ctClassForClass(dmoClass);
		
		collateProperties(baseCtClass);
		
		CtClass wrapperCtClass = classPool.makeClass(className + "_DMWrapper", baseCtClass);
		
		try {
			addGetClassHolderMethod(wrapperCtClass);
			addCommonFields(wrapperCtClass);
			addPropertyFields(wrapperCtClass);
			addConstructor(wrapperCtClass);
			addInitMethod(baseCtClass, wrapperCtClass);
			addGroupInitMethods(wrapperCtClass);
			addWrapperGetters(wrapperCtClass);
			
			Class<?> wrapperClass  = wrapperCtClass.toClass();
			injectClassHolder(wrapperClass);
			
			wrapperConstructor = (Constructor<? extends T>) wrapperClass.getDeclaredConstructors()[0]; 
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling wrapper for " + className, e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Cannot look up class compiling wrapper for " + className, e);
		}
	}

	@SuppressWarnings("unchecked")
	public static DMClassHolder<?, ? extends DMObject<?>> createForClass(DataModel model, Class<?> clazz) {
		return new DMClassHolder(model, DMClassInfo.getForClass(clazz));
	}
	
	private class PropertyGroup {
		public List<DMPropertyHolder<K,T,?>> properties = new ArrayList<DMPropertyHolder<K,T,?>>();
		public boolean initMain = true;
		public String initMethod = null;
		int index;
		int level;
		
		public PropertyGroup(int index) {
			this.index = index;
			this.level = 0;
		}
		
		/* This is used to make a copy of a group in a parent class */
		public PropertyGroup(PropertyGroup other, int level) {
			this.properties = other.properties;
			this.initMain = other.initMain;
			this.initMethod = other.initMethod;
			this.index = other.index;
			this.level = level;
		}
		
		public void addProperty(DMPropertyHolder<K,T,?> property) {
			properties.add(property);
		}
		
		public void setInitMethod(CtMethod method, DMInit annotation) {
			try {
				if (method.getParameterTypes().length != 0)
					throw new RuntimeException(dmoClass.getName() + ": @DMInit method cannot have parameters");
			} catch (NotFoundException e) {
				throw new RuntimeException(dmoClass.getName() + ": Problem looking up parameters for @DMInit method", e);
			}
			
			if (initMethod != null)
				throw new RuntimeException(dmoClass.getName() + ": Cannot add DMInit method " + method.getName() + " for group " + index + " already have method " + initMethod);
			
			initMethod = method.getName();
			initMain = annotation.initMain();
		}

		public int getIndex() {
			return index;
		}
		
		public int getLevel() {
			return level;
		}

		public boolean getInitMain() {
			return initMain;
		}

		public String getInitMethod() {
			return initMethod;
		}

		public List<DMPropertyHolder<K, T, ?>> getProperties() {
			return properties;
		}
	}
}
