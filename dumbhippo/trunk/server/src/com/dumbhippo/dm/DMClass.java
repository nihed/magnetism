package com.dumbhippo.dm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.identity20.Guid;

public class DMClass<T> {
	@SuppressWarnings("unused")
	static private Logger logger = GlobalSetup.getLogger(DMClass.class);
	
//	private DMCache cache;
	private Class<T> clazz;
	private Class<?> keyClass;
	private Constructor keyConstructor;

	public DMClass(DMCache cache, Class<T> clazz) {
//		this.cache = cache;
		this.clazz = clazz;

		for (Constructor c : clazz.getConstructors()) {
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
			keyClass = parameterTypes[0];
		}
	}

	// FIXME: do we need this?
	public Class<?> getKeyClass() {
		return keyClass;
	}
	
	public T createInstance(Object key) {
		try {
			@SuppressWarnings("unchecked")
			T result = (T)keyConstructor.newInstance(key);
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Error creating instance of class " + clazz.getName(), e);
		}
	}
	
	public void processInjections(DMSession session, T t) {
		for (Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Inject.class)) {
				injectField(session, t, field);
			}
		}
	}

	private void injectField(DMSession session, T t, Field field) {
		if (field.getType().equals(EntityManager.class)) {
			setField(t, field, session.getInjectableEntityManager());
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
}
