package com.dumbhippo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;

public class SortUtils {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(SortUtils.class);
	
	/**
	 * Sorts an array of objects based on the property returned by the
	 * specified method invoked on those objects. If the returned property 
	 * is a String, the sort is not case sensitive. The returned property has
	 * to be a Comparable<U> object.
	 * 
	 * @param <T, U>
	 * @param objects an array of objects of type T
	 * @param methodName name of the method to invoke
	 * @return a sorted list
	 */
	public static <T, U> List<T> sortCollection(T[] objects, String methodName) {
		List<T> list = Arrays.asList(objects);
		if (list.isEmpty())
			return list;
		
		try {
			final Method sortByProperty = list.get(0).getClass().getMethod(methodName);			
			final Collator collator = Collator.getInstance();
			Collections.sort(list, new Comparator<T>() {
				public int compare (T t1, T t2) {
					try {
						if (sortByProperty.getReturnType().equals(String.class)) {
					        return collator.compare(((String)sortByProperty.invoke(t1)).toLowerCase(), 
					        		                ((String)sortByProperty.invoke(t2)).toLowerCase());
						} else {
							@SuppressWarnings("unchecked")
							int result = ((Comparable<U>)sortByProperty.invoke(t1)).compareTo((U)sortByProperty.invoke(t2));
							return result;
						}
					} catch (IllegalAccessException e) {
						throw new RuntimeException("method " + sortByProperty + "can't be accessed", e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					}
				}
			});

			return list;		

		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

}