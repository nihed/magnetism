package com.dumbhippo.web;

import com.dumbhippo.server.EJBUtil;

public class WebEJBUtil {
	public static <T> T defaultLookup(Class<T> clazz) {
		if (clazz.isAnnotationPresent(BanFromWebTier.class)) {
			throw new RuntimeException("Class " + clazz.getCanonicalName() + " has BanFromWebTier annotation");
		}
		return EJBUtil.defaultLookup(clazz);
	}
}