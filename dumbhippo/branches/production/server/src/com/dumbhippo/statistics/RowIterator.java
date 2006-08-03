package com.dumbhippo.statistics;

import java.util.Date;
import java.util.Iterator;

/**
 * Specialization of the standard Java iterator allowing you to look
 * ahead and peek the date of the next row that the iterator would return.
 * @author otaylor
 */
public interface RowIterator extends Iterator<Row> {
	/** 
	 * Gets the date of the next row returnred by the iterator 
	 * @return the Date, or null if hasNext() is false
	 */
	Date nextDate();
}
