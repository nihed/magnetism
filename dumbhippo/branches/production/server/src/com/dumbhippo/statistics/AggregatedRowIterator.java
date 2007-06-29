package com.dumbhippo.statistics;

import java.util.Date;

/**
 * A row iterator that wraps an underlying RowIterator that returns fine-grained
 * data and aggregates the fine-grained data into coarser timescales.  
 * @author otaylor
 */
class AggregatedRowIterator implements RowIterator {
	
	RowIterator baseIterator;
	ColumnType[] columnTypes;
	int[] columns;
	long startTime;
	Timescale timescale;
	
	/**
	 * @param baseIterator iterator returning the data to aggregate at a larger timescale
	 * @param columnMap information about the columns returned by the fine-grained iterator
	 * @param columns which clumns we should aggregate
	 * @param start the start time for this iterator; the combination of this and
	 *   the timescale (sampling interval) will determine at which times we return
	 *   data.
	 * @param timescale the new timescale at which to aggregate the data
	 */
	public AggregatedRowIterator(RowIterator baseIterator, ColumnMap columnMap, int[] columns, Date start, Timescale timescale) {
		this.baseIterator = baseIterator;
		this.columns = columns;
		this.startTime = start.getTime();
		this.timescale = timescale;
		
		columnTypes = new ColumnType[columns.length];
		for (int i = 0; i < columns.length; i++)
			columnTypes[i] = columnMap.get(columns[i]).getType();
	}
	
	public boolean hasNext() {
		return baseIterator.hasNext();
	}
	
	private long interpolateColumn(Row before, Row after, int column, long time) {
		long beforeTime = before.getDate().getTime();
		long beforeValue = before.value(column);
		long afterTime = after.getDate().getTime();
		long afterValue = after.value(column);
		// produce a value at time "time", based on values at beforeTime and endTime, weighted by how close 
		// they are to "time"
		return ((time - beforeTime) * afterValue + (afterTime - time) * beforeValue) / (afterTime - beforeTime);
	}
	
	public Row next() {
		SimpleRow row = new SimpleRow(columns.length);
		int numRows = 0;

		long rowTime = nextDate().getTime();
		long endTime = rowTime + (timescale.getSeconds() * 500);
		
		row.setDate(new Date(rowTime));
		Row beforeRow = null;
		Row afterRow = null;

		do { 
			Row baseRow = baseIterator.next();

			// set the closest before and after rows for this rowTime
			if (baseRow.getDate().getTime() < rowTime)
				beforeRow = baseRow;
			else if (afterRow == null)
				afterRow = baseRow;
			
			
			for (int i = 0; i < columns.length; i++) {
				// for SNAPSHOT data we add up all data points during the interval
				if (columnTypes[i] == ColumnType.SNAPSHOT)  {
					row.setValue(i, row.value(i) + baseRow.value(columns[i]));
				}
			}
			
			numRows++;
			
		} while (baseIterator.hasNext() && baseIterator.nextDate().getTime() < endTime);
		
		for (int i = 0; i < columns.length; i++) {
			if (columnTypes[i] == ColumnType.SNAPSHOT)
				row.setValue(i, row.value(i) / numRows);
			else if (beforeRow != null && afterRow != null)
				row.setValue(i, interpolateColumn(beforeRow, afterRow, columns[i], rowTime));
			else if (beforeRow != null)
				row.setValue(i, beforeRow.value(columns[i]));
			else if (afterRow != null)
				row.setValue(i, afterRow.value(columns[i]));
		}
				
		return row;
	}
	
	private long nextRowStartTime() { 
		long nextBaseTime = baseIterator.nextDate().getTime();
		long nextRow = (nextBaseTime - startTime) / (timescale.getSeconds() * 1000);
		
		return startTime + nextRow * (timescale.getSeconds() * 1000);
	}
	
	public Date nextDate() {
		return new Date(nextRowStartTime() + (timescale.getSeconds() * 500));
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
