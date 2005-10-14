/**
 * 
 */
package com.dumbhippo.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be injected by EjbLink. The field 
 * type must be an EJB interface or EjbLink.
 * 
 * The Scope should not match your bean necessarily.
 * Scope SESSION should be used to share stateful EJBs
 * among multiple web-tier objects in an HTTP session.
 * Scope APPLICATION gives global EJBs. Scope NONE 
 * gives you your own private stuff.
 * 
 * Scope of NONE is a good idea if you are injecting
 * an object that already has the right scope and
 * you don't need to share the injected object with anyone 
 * else.
 * 
 * It's possible that sharing objects with SESSION or APPLICATION
 * would be a memory/speed optimization, but it's also possible
 * that it isn't due to thread locking and hash lookups; I have 
 * no idea really...
 * 
 * @author hp
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Inject {
	Scope value() default Scope.NONE; 
}
