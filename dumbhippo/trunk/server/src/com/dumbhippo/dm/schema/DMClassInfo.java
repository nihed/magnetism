package com.dumbhippo.dm.schema;

import java.lang.reflect.Constructor;

import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.identity20.Guid;

public class DMClassInfo<K, T extends DMObject<K>> {
	private Class<K> keyClass;
	private Class<T> objectClass;

	private DMClassInfo(Class<K> keyClass, Class<T> objectClass) {
		this.keyClass = keyClass;
		this.objectClass = objectClass;
	}
	
	public Class<K> getKeyClass() {
		return keyClass;
	}

	public Class<T> getObjectClass() {
		return objectClass;
	}

	@SuppressWarnings("unchecked")
	private static <K,T extends DMObject<K>> Class<T> classCast(Class<K> keyClass, Class<?> elementClass) {
		return (Class<T>)elementClass;
	}

	@SuppressWarnings("unchecked")
	public static DMClassInfo<?,?> getForClass(Class<?> clazz) {
		if (!DMObject.class.isAssignableFrom(clazz))
			return null;
		
		Constructor constructor = findKeyConstructor(clazz);
		Class<?> keyClass = constructor.getParameterTypes()[0]; 
		return new DMClassInfo(keyClass, classCast(keyClass, clazz));
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
}
