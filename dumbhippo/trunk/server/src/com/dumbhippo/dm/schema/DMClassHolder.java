package com.dumbhippo.dm.schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.identity20.Guid;

public class DMClassHolder<T extends DMObject> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DMClassHolder.class);
	
	private DataModel model;
	private Class<T> baseClass;
	private Class<?> keyClass;
	private Constructor keyConstructor;
	private Constructor wrapperConstructor;
	private DMPropertyHolder[] properties;
	private boolean[] mustQualify;
	private Map<String, Integer> propertiesMap = new HashMap<String, Integer>();
	private DMO annotation;

	public DMClassHolder(DataModel model, Class<T> clazz) {
		this.model = model;
		this.baseClass = clazz;
		
		annotation = clazz.getAnnotation(DMO.class);
		if (annotation == null)
			throw new RuntimeException("DMObject class " + clazz.getName() + " doesn't have a @DMO annotation");
		
		// Validate the classId as an URI
		try {
			URI uri = new URI(annotation.classId());
			if (uri.getFragment() != null)
				throw new RuntimeException(clazz.getName() + ": classId '" + annotation.classId() + "' can't have a fragment identifier");

		} catch (URISyntaxException e1) {
			throw new RuntimeException(clazz.getName() + ": classId '" + annotation.classId() + "' is not a valid URI");
		}
		
		keyConstructor = findKeyConstructor(clazz);
		
		keyClass = keyConstructor.getParameterTypes()[0];

		buildWrapperClass();
	}
	
	private static Constructor findKeyConstructor(Class<?> clazz) {
		Constructor keyConstructor = null;
		
		for (Constructor c : clazz.getDeclaredConstructors()) {
			Class<?>[] parameterTypes = c.getParameterTypes();
			if (parameterTypes.length != 1)
				continue;
			
			if (!(parameterTypes[0].equals(Guid.class) ||
				  parameterTypes[0].equals(String.class) ||
				  DMKey.class.isAssignableFrom(parameterTypes[0])))
				  continue;
			
			if (keyConstructor != null)
				throw new RuntimeException("Multiple candidate constructors found for class " + 
										   clazz.getName() + ": " +
										   keyConstructor.toGenericString() + ", " +
										   c.toGenericString());
			
			keyConstructor = c;
		}

		if (keyConstructor == null)
			throw new RuntimeException("No candidate constructors found for class " + clazz.getName());
		
		return keyConstructor;
	}
	
	public static Class<?> findKeyClass(Class<?> clazz) {
		return findKeyConstructor(clazz).getParameterTypes()[0];
	}
	
	public void complete() {
		for (DMPropertyHolder property : properties)
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

	public Class<?> getKeyClass() {
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
	
	public int getPropertyCount() {
		return properties.length;
	}
	
	public DMPropertyHolder getProperty(int propertyIndex) {
		return properties[propertyIndex];
	}
	
	public DMPropertyHolder[] getProperties() {
		return properties;
	}
	
	public boolean mustQualifyProperty(int propertyIndex) {
		return mustQualify[propertyIndex];
	}

	public T createInstance(Object key, DMSession session) {
		try {
			@SuppressWarnings("unchecked")
			T result = (T)wrapperConstructor.newInstance(key, session);
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Error creating instance of class " + baseClass.getName(), e);
		}
	}
	
	public void processInjections(DMSession session, T t) {
		for (Field field : baseClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Inject.class)) {
				injectField(session, t, field);
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
		CtMethod wrapperMethod = new CtMethod(CtClass.voidType, "_dm_init", new CtClass[] {}, wrapperCtClass);
		Template body = new Template(
			"{" +
			"    if (!_dm_initialized) {" +
			"        _dm_session.internalInit($0);" +
			"        _dm_initialized = true;" +
			"    }" +
			"}");
		wrapperMethod.setBody(body.toString());
		
		wrapperCtClass.addMethod(wrapperMethod);
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
	
	private void addWrapperGetter(CtClass baseCtClass, CtClass wrapperCtClass, DMPropertyHolder property, int propertyIndex) throws CannotCompileException, NotFoundException {
		CtField field;
			
		field = new CtField(CtClass.booleanType, "_dm_" + property.getName() + "Initialized", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);

		field = new CtField(property.getCtClass(), "_dm_" + property.getName(), wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
		
		CtMethod wrapperMethod = new CtMethod(property.getCtClass(), property.getMethodName(), new CtClass[] {}, wrapperCtClass);
		
		// TODO: Deal with primitive types, where we need to box/unbox
		
		Template body = new Template(
			"{" +
			"    if (!_dm_%propertyName%Initialized) {" +
			"    	 try {" +
			"           _dm_%propertyName% = %unboxPre%_dm_session.fetchAndFilter(getStoreKey(), %propertyIndex%)%unboxPost%;" +
			"    	 } catch (com.dumbhippo.dm.NotCachedException e) {" +
			"           _dm_init();" +
			"           _dm_%propertyName% = %unboxPre%_dm_session.storeAndFilter(getStoreKey(), %propertyIndex%, %boxPre%super.%methodName%()%boxPost%)%unboxPost%;" +
			"        }" +
			"        _dm_%propertyName%Initialized = true;" +
			"    }" +
			"    return _dm_%propertyName%;" +
			"}");

		body.setParameter("propertyName", property.getName());
		body.setParameter("boxPre", property.getBoxPrefix());
		body.setParameter("boxPost", property.getBoxSuffix());
		body.setParameter("unboxPre", property.getUnboxPrefix());
		body.setParameter("unboxPost", property.getUnboxSuffix());
		body.setParameter("propertyIndex", Integer.toString(propertyIndex));
		body.setParameter("methodName", property.getMethodName());
		wrapperMethod.setBody(body.toString());
		
		wrapperCtClass.addMethod(wrapperMethod);
	}

	private void addWrapperGetters(CtClass baseCtClass, CtClass wrapperCtClass) throws CannotCompileException, NotFoundException {
		List<DMPropertyHolder> foundProperties = new ArrayList<DMPropertyHolder>();
		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		
		for (CtMethod method : baseCtClass.getMethods()) {
			DMPropertyHolder property = DMPropertyHolder.getForMethod(this, method);
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
		
		properties = foundProperties.toArray(new DMPropertyHolder[foundProperties.size()]);
		mustQualify = new boolean[foundProperties.size()];

		for (int i = 0; i < properties.length; i++) {
			DMPropertyHolder property = properties[i];
			
			propertiesMap.put(property.getName(), i);
			mustQualify[i] = nameCount.get(property.getName()) > 1;
			addWrapperGetter(baseCtClass, wrapperCtClass, property, i);
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
		
		CtClass wrapperCtClass = classPool.makeClass(className + "_DMWrapper", baseCtClass);
		
		try {
			addCommonFields(wrapperCtClass);
			addConstructor(wrapperCtClass);
			addInitMethod(baseCtClass, wrapperCtClass);
			addGetClassHolderMethod(wrapperCtClass);
			addWrapperGetters(baseCtClass, wrapperCtClass);
			
			Class<?> wrapperClass  = wrapperCtClass.toClass();
			injectClassHolder(wrapperClass);
			
			wrapperConstructor = wrapperClass.getDeclaredConstructors()[0]; 
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling wrapper for " + className, e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Cannot look up class compiling wrapper for " + className, e);
		}
	}
}
