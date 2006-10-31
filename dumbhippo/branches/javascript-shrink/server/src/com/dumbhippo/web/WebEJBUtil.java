package com.dumbhippo.web;

import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.server.util.EJBUtil;

public class WebEJBUtil {
	public static <T> T defaultLookup(Class<T> clazz) {
		if (clazz.isAnnotationPresent(BanFromWebTier.class)) {
			throw new RuntimeException("Class " + clazz.getCanonicalName() + " has BanFromWebTier annotation");
		}
		return uncheckedDefaultLookup(clazz);
	}
	
	public static <T> T uncheckedDefaultLookup(Class<T> clazz) {
		return EJBUtil.defaultLookup(clazz);
	}
}