package com.dumbhippo.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this annotation is set on a field of a class instantiated with
 * <dh:bean> then the field will be filled in with the SigninBean
 * from the session or a newly created SigninBean.
 * 
 * @author otaylor
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Signin {
}
