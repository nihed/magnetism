package com.dumbhippo.dm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation applied to a method of a baseclass of an inheritance heirarchy of
 * DMObject's to indicate that the method takes a key as an argument, and
 * returns the class of the DMObject to construct for the key.   
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MetaConstruct {
}
