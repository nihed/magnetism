package com.dumbhippo.jive.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to provide information about handler-wide options
 * for a subclass of AbstractIQHandler. 
 * 
 * Using an annotation here rather than abstract methods might be considered
 * a little strange, but it the advantage of being more concise and declarative.
 *  
 * @author otaylor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IQHandler {
	/**
	 * @return the namespace of IQ subelements that this handler handles
	 */
	public String namespace();	
}
