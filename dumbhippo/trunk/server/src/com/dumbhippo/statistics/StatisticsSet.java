package com.dumbhippo.statistics;

import java.util.Date;
import java.util.Iterator;

/**
 * This abstract class represents a set of recorded statistics data that can be
 * queried for the description of the columns and for the data itself.
 *  
 * @author otaylor
 */
public abstract class StatisticsSet {
	protected Header header;
	protected String filename;
	protected RowStore rowStore;
	
	/**
	 * Get the hostname on which the data was recorded
	 * @return the hostname
	 */
	public String getHostName() {
		return header.getHostName();
	}
	
	/**
	 * Get the date and time at which data recording was started
	 * @return the start timestamp
	 */
	public Date getStartDate() {
		return header.getStartTime();
	}
	
	/**
	 * Get the date and time at which data recording ended
	 * @return the end timestamp
	 */
	public Date getEndDate() {
		return new Date(header.getStartTime().getTime() + header.getInterval() * (header.getNumRecords() - 1));
	}
	
	/**
	 * Get the path to the statistics set for identifying purposes 
	 * @return the path to the set
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Return whether the set is the set where statistics are currently
	 * being stored. Clients may want to act differently in that case
	 * (for example, periodically update their display.)
	 * @return true if the set is the set being currently updated
	 */
	abstract public boolean isCurrent();
	
	/** Get information about the columns stored in the statistics set
	 * @return a ColumnMap object holding information about the column
	 */
	public ColumnMap getColumns() {
		return header.getColumns();
	}
	
	/**
	 * Get an iterator to the data held in the statistics set; the handling of 
	 *  startDate and endDate is inclusive; if possible we return a set of 
	 *  points that spans the entire range.
	 * @param startDate time to start the iterator at
	 * @param endDate end time for the iterator
	 * @param timescale timescale (interval )at which to return data. If you select a 
	 *   larger timescale than the timescale at which the data was recorded, then 
	 *   the fine-grained data will be aggregated to the larger timescale 
	 * @param columnIndexes the columns from the data set to include in the output;
	 *   restricting the set of columns included will reduce the size of the
	 *   data set, and will also make aggregation to larger timescales faster
	 * @return
	 */
	public Iterator<Row> getIterator(Date startDate, Date endDate, Timescale timescale, int[] columnIndexes) {
	    long interval = timescale.getSeconds() * 1000;
		long fileStartTime = getStartDate().getTime();
		long fileEndTime = getEndDate().getTime();
		long startTime = startDate.getTime();
		long endTime = endDate.getTime();
		
		if (startTime < fileStartTime)
			startTime = fileStartTime;
		if (endTime > fileEndTime)
			endTime = fileEndTime;

		long startIndex = (startTime - fileStartTime) / interval;
		// round up; add 1 because the endTime is inclusive, but the
		// rowStore.getiterator() doesn't include the last row
		long endIndex = 1 + (endTime + interval - 1 - fileStartTime) / interval;
		
		long factor = (timescale.getSeconds() * 1000) / header.getInterval();
		
		long startRow = startIndex * factor;
		// endRow might end up being greater than the number of rows we actually have, because
		// if requested interval is greater than INTERVAL, the last aggregated data point
		// can be based on fewer original data points, i.e. if we are planning to produce 
		// 2 points on the 60 second timescale, and our original timescale is 15 seconds,
		// 2*4 will give us 8 for endRow, while we might only have 6 original data points
		//
		// rowStore.getIterator() will handle the endRow value that is greater than the 
		// number of rows fine
		long endRow = endIndex * factor;
		RowIterator baseIterator = rowStore.getIterator(startRow, endRow);

		if (factor == 1)
			return new FilteredRowIterator(baseIterator, columnIndexes);
		else {
			Date resultStartDate = new Date(fileStartTime + startIndex * interval);
			return new AggregatedRowIterator(baseIterator, getColumns(), columnIndexes, resultStartDate, timescale);
		}
	}
}
