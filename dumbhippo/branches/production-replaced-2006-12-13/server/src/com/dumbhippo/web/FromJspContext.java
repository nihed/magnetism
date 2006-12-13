package com.dumbhippo.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this annotation is set on a field of a class instantiated with
 * <dh:bean> then the field will be filled in with the requested 
 * attribute from the given scope.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FromJspContext {
	public String value();
	public Scope scope();
}
