package com.dumbhippo.statistics;

/**
 * Provide information about the contents of a column
 */
public interface ColumnDescription {
	/**
	 * Return a string uniquely identifying this column. This should distinguish
	 * the column from any other column that might be in the data set,
	 * so use a specific name like "jspPageErrors", rather than just "errors" 
	 * @return a string uniquely identifying this column
	 */
	String getId();
	
	/**
	 * Return the name for this column that will be displayed to the user. For example
	 * "JSP Page Errors"
	 * @return The name for the column
	 */
	String getName();
	
	/**
	 * Return the units for this column. A column might, for example, represent seconds,
	 * bytes of memory, or a count of events.
	 * @return units for this column
	 */
	ColumnUnit getUnits();
	
	/**
	 * Return a value describing the way the data for this column is collected.   
	 * @return the type of the column 
	 */
	ColumnType getType();
}
