package com.dumbhippo.web.tags;

public class FuncUtils {
	public static boolean enumIs(Enum value, String name) {
		return value.name().equals(name);
	}
	
	public static boolean myInstanceOf(Object obj, String className) throws ClassNotFoundException {
		Class supposedClass = Class.forName(className);
		return obj.getClass().isAssignableFrom(supposedClass);
	}
}
