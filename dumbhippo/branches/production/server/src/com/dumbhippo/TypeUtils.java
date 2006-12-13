package com.dumbhippo;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TypeUtils {

	/**
	 * This is a no-op, just to remove the compiler warning about casting 
	 * a list of Object to list of something else.
	 * Otherwise you have to use SuppressWarnings("unchecked") all over the 
	 * place, which can hide other kinds of warning.
	 * Also, when debugging, this method could be made to runtime check the 
	 * types of the list elements.
	 * 
	 * @param klass the class of each list element
	 * @param list the list
	 * @return the list (not copied or modified in any way)
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> castList(Class<T> klass, List list) {
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Set<T> castSet(Class<T> klass, Set set) {
		return set;
	}
	
	public static <T> List<T> emptyList(Class<T> klass) {
		return TypeUtils.castList(klass, Collections.emptyList());
	}
	
	public static <T> Set<T> emptySet(Class<T> klass) {
		return TypeUtils.castSet(klass, Collections.emptySet());
	}
}
