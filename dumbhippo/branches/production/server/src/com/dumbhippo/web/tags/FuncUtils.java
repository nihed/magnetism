package com.dumbhippo.web.tags;

import java.util.Random;

import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;

public class FuncUtils {
	public static boolean enumIs(Enum value, String name) {
		if (value == null)
			throw new IllegalArgumentException("null enum value passed to enumIs function, name=" + name);
		if (name == null)
			throw new IllegalArgumentException("null name passed to enumIs function, value=" + value);
		return value.name().equals(name);
	}
	
	public static boolean myInstanceOf(Object obj, String className) throws ClassNotFoundException {
		if (obj == null)
			throw new IllegalArgumentException("null object in myInstanceOf checking for class " + className);
		if (className == null)
			throw new IllegalArgumentException("null class name in myInstanceOf object is " + obj);
		Class<?> supposedClass = Class.forName(className);
		return supposedClass.isAssignableFrom(obj.getClass());
	}
	
	public static String format1(String format, Object o) {
		return String.format(format, o);
	}
	
	public static int randomInt(int belowThis) {
		return new Random().nextInt(belowThis);
	}
	
	public static String urlEncode(String s) {
		return StringUtils.urlEncode(s);
	}
	
	public static String xmlEscape(String s) {
		return XmlBuilder.escape(s);
	}
}
