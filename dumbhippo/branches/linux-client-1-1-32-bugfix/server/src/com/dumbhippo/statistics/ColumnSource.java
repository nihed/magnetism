package com.dumbhippo.statistics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used to track a single source that is providing a statistic.
 * One is created for each annotated method of each active {@link StatisticsSource}.
 *  
 * @author otaylor
 */
class ColumnSource implements ColumnDescription {
	private StatisticsSource source;
	private Method method;
	private Column annotation;
	
	private ColumnSource(StatisticsSource source, Method method, Column annotation) {
		this.source = source;
		this.method = method;
		this.annotation = annotation;
	}
	
	/**
	 * See if a ColumnSource should be created for a particular method by checking
	 * for an appropriate Column annotation on the method. If the Column annotation
	 * is found, a new ColumnSource object is created and returned.
	 * @param source the object on which the method was found
	 * @param method the method to check
	 * @return the new ColumnSource object, or null. 
	 */
	static public ColumnSource forMethod(StatisticsSource source, Method method) {
		Column annotation = method.getAnnotation(Column.class);
		if (annotation == null)
			return null;
		
		return new ColumnSource(source, method, annotation);
	}
	
	/**
	 * Get the current value of the source
	 * @return the current source value
	 */
	public long getValue() {
		try {
			return (Long)method.invoke(source);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can't access column source getter");
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Exception invoking column source getter", e);
		}
	}
	
	public String getId() {
		return annotation.id();
	}

	public String getName() {
		return annotation.name();
	}
	
	public ColumnUnit getUnits() {
		return annotation.units();
	}
	
	public ColumnType getType() {
		return annotation.type();
	}
}
