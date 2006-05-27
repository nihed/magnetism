package com.dumbhippo.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Options that can be set on a http method. If invalidatesSession
 * is true, that means that on succesfull completin of the method,
 * the session should be invalidated. The session is not invalidated
 * if the method throws an exception.
 * 
 * @author otaylor
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface HttpOptions {
	boolean invalidatesSession() default false;
	boolean allowDisabledAccount() default false;
	boolean adminOnly() default false;	
	String[] optionalParams() default {};
}
