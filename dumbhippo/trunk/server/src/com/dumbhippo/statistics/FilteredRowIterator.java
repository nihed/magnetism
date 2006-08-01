package com.dumbhippo.statistics;

import java.util.Date;

/**
 * Row iterator that wraps an underlying iterator and returns a subset of columns
 * @author otaylor
 */
class FilteredRowIterator implements RowIterator {
	RowIterator baseIterator;
	int[] columns;
	long startTime;
	Timescale timescale;
	
	public FilteredRowIterator(RowIterator baseIterator, int[] columns) {
		this.baseIterator = baseIterator;
		this.columns = columns;
	}
	
	public boolean hasNext() {
		return baseIterator.hasNext();
	}
	
	public Row next() {
		return new FilteredRow(baseIterator.next());
	}
	
	public Date nextDate() {
		return baseIterator.nextDate();
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	private class FilteredRow extends Row {
		Row baseRow;
		
		public FilteredRow(Row baseRow) {
			this.baseRow = baseRow;
		}
		
		public Date getDate() {
			return baseRow.getDate();
		}
		
		public long value(int column) {
			return baseRow.value(columns[column]);
		}
		
		public int numColumns() {
			return columns.length;
		}
	}
}
