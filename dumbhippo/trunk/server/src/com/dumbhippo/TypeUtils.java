package com.dumbhippo;
import java.util.List;

public class TypeUtils {

	@SuppressWarnings("unchecked")
	public static <T> List<T> castList(Class<T> klass, List list) {
		return list;
	}
}
