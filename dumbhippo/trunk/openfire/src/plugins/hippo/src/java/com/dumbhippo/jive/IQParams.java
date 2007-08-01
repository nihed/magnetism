package com.dumbhippo.jive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides a mapping between parameter names of a query IQ and the parameters
 * of the method used to service it. (This is needed because parameter names
 * aren't retained at runtime) There should be one element per parameter
 * (excluding any UserViewpoint parameters). Defaults can also be provided
 * by using values of the form "includeChildren=false".
 * 
 * @author otaylor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IQParams {
	String[] value();
}
