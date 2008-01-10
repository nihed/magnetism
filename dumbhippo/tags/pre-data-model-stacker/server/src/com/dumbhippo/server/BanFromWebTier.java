package com.dumbhippo.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated item should not be 
 * used in the web tier. The EjbLink class 
 * in the web tier filters these out.
 * 
 * @author hp
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface BanFromWebTier {

}

