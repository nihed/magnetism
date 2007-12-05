package com.dumbhippo.dm.schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.Inject;
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
	private Class<T> baseClass;
	private Class<K> keyClass;
	private Constructor<K> keyStringConstructor;
	private Constructor<? extends T> wrapperConstructor;
	private DMPropertyHolder<K,T,?>[] properties;
	private boolean[] mustQualify;
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
		baseClass = classInfo.getObjectClass();
		keyClass = classInfo.getKeyClass();
		
		if (keyClass != Guid.class && keyClass != String.class && keyClass != Long.class) {
			try {
				keyStringConstructor = keyClass.getConstructor(String.class);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(baseClass.getName() + ": Can't find constructor for key class from a string");
			}
			
			for (Class<?> exceptionClass : keyStringConstructor.getExceptionTypes()) {
				if (!(RuntimeException.class.isAssignableFrom(exceptionClass) ||
					  BadIdException.class.isAssignableFrom(exceptionClass)))
					throw new RuntimeException(baseClass.getName() + ": String key constructor can only throw BadIdException");
			}
		}
		
		annotation = baseClass.getAnnotation(DMO.class);
		if (annotation == null)
			throw new RuntimeException("DMObject class " + baseClass.getName() + " doesn't have a @DMO annotation");
		
		// Validate the classId as an URI
		try {
			URI uri = new URI(annotation.classId());
			if (uri.getFragment() != null)
				throw new RuntimeException(baseClass.getName() + ": classId '" + annotation.classId() + "' can't have a fragment identifier");

		} catch (URISyntaxException e1) {
			throw new RuntimeException(baseClass.getName() + ": classId '" + annotation.classId() + "' is not a valid URI");
		}
		

		buildWrapperClass();
		
		DMFilter filterAnnotation = baseClass.getAnnotation(DMFilter.class);
		if (filterAnnotation != null) {
			try {
				filter = FilterParser.parse(filterAnnotation.value());
			} catch (com.dumbhippo.dm.parser.ParseException e) {
				throw new RuntimeException(baseClass.getName() + ": Error parsing filter", e);
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
		return annotation.resourceBase();
	}
	
	public String getClassId() {
		return annotation.classId();
	}

	public Class<K> getKeyClass() {
		return keyClass;
	}
	
	public Class<T> getBaseClass() {
		return baseClass;
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

	public T createInstance(Object key, DMSession session) {
		try {
			T result = wrapperConstructor.newInstance(key, session);
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Error creating instance of class " + baseClass.getName(), e);
		}
	}
	
	public void processInjections(DMSession session, T t) {
		for (Field field : baseClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Inject.class)) {
				injectField(session, t, field);
			} else if (field.isAnnotationPresent(EJB.class)) {
				Object bean = EJBUtil.defaultLookup(field.getType());
				setField(t, field, bean);			
			}
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
		return model.getBaseUrl() + annotation.resourceBase() + "/" + key.toString();
	}
	
	public String makeRelativeId(K key) {
		return annotation.resourceBase() + "/" + key.toString();
	}

	public StoreKey<K,T> makeStoreKey(String string) throws BadIdException {
		if (keyClass == Guid.class) {
			try {
				Guid guid = new Guid(string);
				
				@SuppressWarnings("unchecked")
				StoreKey<K,T> key = new StoreKey(this, guid);
				return key;
			} catch (ParseException e) {
				throw new BadIdException("Invalid GUID in resourceId");
			}
			
		} else if (keyClass == String.class) {
			@SuppressWarnings("unchecked")
			StoreKey<K,T> key = new StoreKey(this, string);
			return key;
		} else if (keyClass == Long.class) {
			try {
				Long l = Long.parseLong(string);
				
				@SuppressWarnings("unchecked")
				StoreKey<K,T> key = new StoreKey(this, l);
				return key;
			} catch (NumberFormatException e) {
				throw new BadIdException("Invalid long in resourceId");
			}
		} else {
			try {
				Object keyObject = keyStringConstructor.newInstance(string);
				
				@SuppressWarnings("unchecked")
				StoreKey<K,T> key = new StoreKey(this, keyObject);
				return key;
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

	private void injectField(DMSession session, T t, Field field) {
		if (field.getType() == EntityManager.class) {
			setField(t, field, session.getInjectableEntityManager());
		} else if (field.getType() == DMSession.class) {
			setField(t, field, session);
		} else if (DMViewpoint.class.isAssignableFrom(field.getType())) {
			// We use a isAssignableFrom check here to allow people to @Inject fields
			// that are subclasses of DMViewpoint. If the type of the @Inject field
			// is a subtype of DMViewpoint not compatible with the the viewpoint of
			// the DMSession, then this we'll get a ClassCastException here
			setField(t, field, session.getViewpoint());
		} else { 
			throw new RuntimeException("@Inject annotation found field of unknown type " + field.getType().getName());
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
	
	private void collateProperties(CtClass baseCtClass) {
		List<DMPropertyHolder<K,T,?>> foundProperties = new ArrayList<DMPropertyHolder<K,T,?>>();
		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		
		for (CtMethod method : baseCtClass.getMethods()) {
			DMPropertyHolder<K,T,?> property = DMPropertyHolder.getForMethod(this, method);
			if (property != null) {
				foundProperties.add(property);
				if (!nameCount.containsKey(property.getName()))
					nameCount.put(property.getName(), 1);
				else
					nameCount.put(property.getName(), 1 + nameCount.get(property.getName()));
			}
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
			"    if (!_dm_initialized) {" +
			"        _dm_session.internalInit($0);" +
			"        _dm_initialized = true;" +
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
	
	private void addGroupInitMethod(CtClass wrapperCtClass, int group, List<DMPropertyHolder<K,T,?>> properties) throws CannotCompileException {
		CtMethod method = new CtMethod(CtClass.voidType, "_dm_initGroup" + group, new CtClass[] {}, wrapperCtClass);
		StringBuilder body = new StringBuilder();
		
		body.append("{" +
					"  _dm_init();");
		
		for (DMPropertyHolder<K,T,?> property : properties) {
			Template propertyInit = new Template(
					"  _dm_%propertyName% = %unboxPre%_dm_session.storeAndFilter(getStoreKey(), %propertyIndex%, %boxPre%super.%methodName%()%boxPost%)%unboxPost%;" +
					"  _dm_%propertyName%Initialized = true;");
			
			propertyInit.setParameter("propertyName", property.getName());
			propertyInit.setParameter("boxPre", property.getBoxPrefix());
			propertyInit.setParameter("boxPost", property.getBoxSuffix());
			propertyInit.setParameter("unboxPre", property.getUnboxPrefix());
			propertyInit.setParameter("unboxPost", property.getUnboxSuffix());
			propertyInit.setParameter("propertyIndex", Integer.toString(getPropertyIndex(property.getName())));
			propertyInit.setParameter("group", Integer.toString(group));
			propertyInit.setParameter("methodName", property.getMethodName());

			body.append(propertyInit.toString());
		}
		
		body.append("}");
		
		method.setBody(body.toString());
		wrapperCtClass.addMethod(method);
	}
	
	private void addGroupInitMethods(CtClass wrapperCtClass) throws CannotCompileException {
		Map<Integer, List<DMPropertyHolder<K,T,?>>> map = new HashMap<Integer, List<DMPropertyHolder<K,T,?>>>();
		
		for (DMPropertyHolder<K,T,?> property : properties) {
			int group = property.getGroup();
			if (group >= 0) {
				List<DMPropertyHolder<K,T,?>> l = map.get(group);
				if (l == null) {
					l = new ArrayList<DMPropertyHolder<K,T,?>>();
					map.put(group, l);
				}
				
				l.add(property);
			}
		}

		for (int group : map.keySet()) {
			addGroupInitMethod(wrapperCtClass, group, map.get(group));
		}
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
		int group = property.getGroup();
		if (group < 0) {
			storeCommands =
				"_dm_init();" +
				"_dm_%propertyName% = %unboxPre%_dm_session.storeAndFilter(getStoreKey(), %propertyIndex%, %boxPre%super.%methodName%()%boxPost%)%unboxPost%;" +
				"_dm_%propertyName%Initialized = true;";
		} else {
			storeCommands = 
				"_dm_initGroup%group%();";
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
		body.setParameter("group", Integer.toString(group));
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
		String className = baseClass.getName();
		ClassPool classPool = model.getClassPool();
		CtClass baseCtClass = ctClassForClass(baseClass);
		
		collateProperties(baseCtClass);
		
		CtClass wrapperCtClass = classPool.makeClass(className + "_DMWrapper", baseCtClass);
		
		try {
			addCommonFields(wrapperCtClass);
			addPropertyFields(wrapperCtClass);
			addConstructor(wrapperCtClass);
			addInitMethod(baseCtClass, wrapperCtClass);
			addGroupInitMethods(wrapperCtClass);
			addGetClassHolderMethod(wrapperCtClass);
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

	// This "casts" two classes K and T to be related, note we need a K and a T in order to construct
	// DMClassHolder without a warning about a bare type
	@SuppressWarnings("unchecked")
	private static <K, T extends DMObject<K>> DMClassHolder<K,T> newClassHolderHack(DataModel model, DMClassInfo<?,?> classInfo) {
		return new DMClassHolder<K,T>(model, (DMClassInfo<K,T>) classInfo);
	}
	
	
	public static DMClassHolder<?, ? extends DMObject<?>> createForClass(DataModel model, Class<?> clazz) {
		return newClassHolderHack(model, DMClassInfo.getForClass(clazz));
	}
}
