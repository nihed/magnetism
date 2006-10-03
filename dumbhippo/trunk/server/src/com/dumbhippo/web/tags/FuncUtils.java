package com.dumbhippo.web.tags;

public class FuncUtils {
	public static boolean enumIs(Enum value, String name) {
		return value.name().equals(name);
	}
}
