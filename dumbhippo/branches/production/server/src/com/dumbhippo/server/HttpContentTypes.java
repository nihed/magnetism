package com.dumbhippo.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Array of content types that a method can return via HTTP.
 * If any content type other than NONE is listed, the method must have the 
 * first two arguments OutputStream out, HttpResponseData contentType
 * 
 * @author hp
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface HttpContentTypes {
	HttpResponseData[] value();
}
