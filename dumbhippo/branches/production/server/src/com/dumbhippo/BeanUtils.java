package com.dumbhippo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class BeanUtils {

	/** 
	 * I don't really know if this is the "correct" behavior for setting bean properties,
	 * but it fills the immediate need... 
	 * 
	 * @param bean object with a setter
	 * @param key the property name corresponding to the setter
	 * @param value the value to set
	 */
	public static void setValue(Object bean, String key, Object value) {
		String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1, key.length());
		Method setter = null;
		for (Class<?> klass = bean.getClass(); klass != null; klass = klass.getSuperclass()) {
			Method[] methods = klass.getMethods();
			for (Method m : methods) {
				if (m.getName().equals(setterName)) {
					setter = m;
					break;
				}
			}
			if (setter != null)
				break;
		}
		if (setter == null)
			throw new RuntimeException("No setter " + setterName + " found on object " + bean.getClass().getName());
		Class<?>[] params = setter.getParameterTypes();
		if (params.length != 1)
			throw new RuntimeException("setter doesn't have exactly 1 argument?");
		Class<?> paramType = params[0];
		Object arg1;
		if (String.class.isAssignableFrom(value.getClass())) {
			// if value is a string, we can try to parse it to various other types
			String s = (String) value;
			if (paramType.isAssignableFrom(String.class))
				arg1 = s;
			else if (paramType.isAssignableFrom(Long.class) || paramType.isAssignableFrom(long.class))
				arg1 = Long.parseLong(s);
			else if (paramType.isAssignableFrom(Integer.class) || paramType.isAssignableFrom(int.class))
				arg1 = Integer.parseInt(s);
			else if (paramType.isAssignableFrom(Boolean.class) || paramType.isAssignableFrom(boolean.class))
				arg1 = Boolean.parseBoolean(s);
			else
				throw new RuntimeException("don't yet support converting string to type " + paramType.getName());
		} else {
			if (!paramType.isAssignableFrom(value.getClass()))
				throw new RuntimeException("can't set property of type " + paramType.getName() + " to value of type " + value.getClass().getName());
			arg1 = value;
		}
		
		try {
			setter.invoke(bean, arg1);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("paramType " + paramType.getName() + " value type " + arg1.getClass().getName(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("method " + setterName, e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
