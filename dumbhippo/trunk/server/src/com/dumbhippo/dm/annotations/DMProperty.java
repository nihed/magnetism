package com.dumbhippo.dm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DMProperty {
	boolean defaultInclude() default false;
	String propertyId() default "";
	String defaultChildren() default "";
}
