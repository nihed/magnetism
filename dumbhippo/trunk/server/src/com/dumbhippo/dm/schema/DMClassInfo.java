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

	// This "casts" two classes K and T to be related, note we need a K and a T in order to construct
	// DMClassInfo without a warning about a bare type
	@SuppressWarnings("unchecked")
	private static <K, T extends DMObject<K>> DMClassInfo<K,T> newClassInfoHack(Class<?> kClass, Class<?> tClass) {
		return new DMClassInfo<K,T>((Class<K>)kClass, (Class<T>)tClass);
	}
	
	public static DMClassInfo<?, ? extends DMObject<?>> getForClass(Class<?> tClass) {
		if (!DMObject.class.isAssignableFrom(tClass))
			return null;
		
		Constructor<?> constructor = findKeyConstructor(tClass);
		Class<?> keyClass = constructor.getParameterTypes()[0]; 
		return newClassInfoHack(keyClass, tClass);
	}
	
	private static <ConstructedType> Constructor<ConstructedType> findKeyConstructor(Class<ConstructedType> clazz) {
		Constructor<ConstructedType> keyConstructor = null;
		
		for (Constructor<ConstructedType> c : clazz.getDeclaredConstructors()) {
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
