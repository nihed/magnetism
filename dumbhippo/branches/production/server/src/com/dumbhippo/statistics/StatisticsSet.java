package com.dumbhippo.statistics;

import java.util.Date;
import java.util.Iterator;

/**
 * This interface represents a set of recorded statistics data that can be
 * queried for the description of the columns and for the data itself.
 *  
 * @author otaylor
 */
public interface StatisticsSet {
	/**
	 * Get the hostname on which the data was recorded
	 * @return the hostname
	 */
	public String getHostName();
	
	/**
	 * Get the date and time at which data recording was started
	 * @return the start timestamp
	 */
	public Date getStartDate();
	
	/**
	 * Get the date and time at which data recording ended
	 * @return the end timestamp
	 */
	public Date getEndDate();
	
	/**
	 * Get the path to the statistics set for identifying purposes 
	 * @return the path to the set
	 */
	public String getFilename();
	
	/** Get information about the columns stored in the statistics set
	 * @return a ColumnMap object holding information about the column
	 */
	public ColumnMap getColumns();
	
	/**
	 * Get an iterator to the data held in the statistics set
	 * @param startDate time to start the iterator at
	 * @param endDate end time for the iterator
	 * @param timescale timescale (interfval )at which to return data. If you select a 
	 *   larger timescale than the timescale at which the data was recorded, then 
	 *   the fine-grained data will be aggregated to the larger timescale 
	 * @param columnIndexes the columns from the data set to include in the output;
	 *   restricting the set of columns included will reduce the size of the
	 *   data set, and will also make aggregation to larger timescales faster
	 * @return
	 */
	public Iterator<Row> getIterator(Date startDate, Date endDate, Timescale timescale, int[] columnIndexes);
}
