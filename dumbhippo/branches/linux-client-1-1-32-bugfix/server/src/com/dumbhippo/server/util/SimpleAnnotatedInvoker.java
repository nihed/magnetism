package com.dumbhippo.server.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Utility class which can invoke a method on 
 * an object by name, filtering the methods by
 * a particular annotation and optionally 
 * transforming the arguments.  Right now only
 * supports Strings for incoming args.  Leet.
 */
public class SimpleAnnotatedInvoker {
	private Class<? extends Annotation> annotation;

	private Object target;

	private ArgumentInterceptor interceptor;

	public interface ArgumentInterceptor {
		public List<Object> interceptArgs(Method method, List<String> args, Object context)
				throws InvocationTargetException;
	}

	public SimpleAnnotatedInvoker(Class<? extends Annotation> name, Object target) {
		this(name, target, null);
	}

	public SimpleAnnotatedInvoker(Class<? extends Annotation> name, Object target, ArgumentInterceptor interceptor) {
		this.annotation = name;
		this.target = target;
		this.interceptor = interceptor;
	}

	public String invoke(String methodName, List<String> args) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		return invoke(methodName, args, null);
	}

	public String invoke(String methodName, List<String> args, Object context) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		Class<?>[] interfaces = target.getClass().getInterfaces();
		for (Class<?> iface : interfaces) {
			Method[] methods = iface.getMethods();
			for (Method method : methods) {
				List<Object> targetArgs;
				if (!method.getName().equals(methodName)) {
					System.err.println("name " + method.getName() + " doesn't match " + methodName);
					continue;
				}
				Annotation expectedAnnotation = method.getAnnotation(annotation);
				if (expectedAnnotation == null) {
					System.err.println("no annotation found for " + methodName);
					continue;
				}
				Class[] paramTypes = method.getParameterTypes();
				if (interceptor != null) {
					targetArgs = interceptor.interceptArgs(method, args, context);
				} else {
					targetArgs = new ArrayList<Object>(args);
				}
				if (paramTypes.length != targetArgs.size())
					throw new IllegalArgumentException("Invalid number of arguments given for method " + methodName);
				Object ret = method.invoke(target, targetArgs.toArray());
				if (ret == null)
					return null;
				if (ret instanceof String)
					return (String) ret;
				return null;
			}
		}
		throw new IllegalArgumentException("no method matched with name " + methodName + " and args "
				+ Arrays.toString(args.toArray()));
	}
}
