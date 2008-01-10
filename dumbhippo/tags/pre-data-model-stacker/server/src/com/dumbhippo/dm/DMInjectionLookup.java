package com.dumbhippo.dm;

import java.lang.annotation.Annotation;

/**
 * This interface allows system creating a DataModel to customize how property
 * injection for DMObject instances works. To process a field for injection,
 * the following steps are taken:
 * 
 *  A) injectionLookup.getInjectionByAnnotations() is called; if it returns
 *     not-null, that value is used
 *  B) if the @Inject annotation is present:
 *    B1) injectionLookup.getInjectionByType is called;  if it returns
 *      not-null, that value is used
 *    B2) default injection types such as DMSession and EntityManager are checked for,
 *      if one is found, the cooresponding value is used 
 *    B3) otherwise, an exception is raised
 *  C) any other built-in annotations are checked for 
 */
public interface DMInjectionLookup {
	/**
	 * Determine the value to inject into a property based on the annotations of the
	 * property.
	 * 
	 * @param valueType the type of the property's value
	 * @param annotations annotations on the property (the field or method)
	 * @return the value to inject into the property, or null if no injection is known 
	 *   based on the annotations.
	 */
	Object getInjectionByAnnotations(Class<?> valueType, Annotation[] annotations, DMSession session);

	
	/**
	 * Determine the value to inject into a property annotated with @Inject
	 * 
	 * @param valueType the type of the property's value
	 * @return the value to inject into the property, or null if no injection is known for
	 *    the type.
	 */
	Object getInjectionByType(Class<?> valueType, DMSession session);
}
