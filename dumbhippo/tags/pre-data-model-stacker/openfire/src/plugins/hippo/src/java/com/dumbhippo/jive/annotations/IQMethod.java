package com.dumbhippo.jive.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.xmpp.packet.IQ;

/**
 * Marks a method as handling a particular IQ child element within a namespace.
 * 
 * TODO: We might want to make name and type default from the name of the method; if 
 * the method name is getBlocks, then automatically use name="blocks", type="get".
 * 
 * @author otaylor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface IQMethod {
	/**
	 * @return The name of the child element
	 */
	public String name();
	
	/**
	 * @return the type of IQ (get or set)
	 */
	public IQ.Type type();
	
	/**
	 * @return whether a transaction should be created to wrap the entire handling
	 *   of the IQ
	 */
	public boolean needsTransaction() default true;
}
