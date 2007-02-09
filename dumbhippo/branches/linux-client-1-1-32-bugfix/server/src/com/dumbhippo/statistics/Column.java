package com.dumbhippo.statistics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking a property getter in a class as a column that
 * should be stored into the statistics data store. See the {@link ColumnDescription}
 * docs for details about the fields of the annotation.
 * @author otaylor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Column {
	String id();
	String name();
	ColumnUnit units() default ColumnUnit.EVENTS;
	ColumnType type() default ColumnType.CUMULATIVE;
}
