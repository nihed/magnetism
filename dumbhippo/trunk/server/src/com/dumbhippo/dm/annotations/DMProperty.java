package com.dumbhippo.dm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DMProperty {
	/**
	 * The ID of the property (an URI); if not specifed, defaults to the value
	 * [classId]#[propertyName].
	 */
	String propertyId() default "";

	/**
	 * The type of the property; this allows distinguishing between different
	 * property types that correspond to the same Java type; for example, between
	 * PropertyType.STRING and PropertyType.URL.
	 * 
	 */
	PropertyType type() default PropertyType.AUTO;

	/**
	 * Whether the property is included as part of the default fetch result
	 * (specified with '+' in a fetch string) 
	 */
	boolean defaultInclude() default false;
	
	/**
	 * When the property is fetched because the fetch string includes a '+',
	 * the fetch string used to fetch children of the property. (Only can
	 * be specified for resource-typed properties, forces defaultInclude on) 
	 */
	String defaultChildren() default "";

	/**
	 * Whether the property should be cached or not. The main reason to avoid
	 * caching a property is if the value depends on the viewer, since values
	 * are cached independent of viewer.
	 */
	boolean cached() default true;
	
	/**
	 * An integer grouping this property together with other properties. When
	 * properties are grouped, a cache miss on one forces all the property
	 * values to be fetched and stored into the cache. You would use grouping
	 * if you have multiple properties whose values are computed at the same
	 * time via some expensive operation. (Make sure that you cache all the
	 * values locally in the DMO after computing them, or you'll just do
	 * the expensive operation N times immediately, rather than lazily.)
	 * The default value of -1 means no grouping. 
	 */
	int group() default -1;
}
