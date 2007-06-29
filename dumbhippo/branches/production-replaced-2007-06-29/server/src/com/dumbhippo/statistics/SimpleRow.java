package com.dumbhippo.statistics;

import java.util.Date;

/**
 * An implementation of {@link Row} that is backed with an array, suitable for
 * temporary creation of a row in memory. 
 * @author otaylor
 */
class SimpleRow extends Row {
	Date date;
	long[] columns;
	
	public SimpleRow(int numColumns) {
		columns = new long[numColumns]; 
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public int numColumns() {
		return columns.length;
	}

	@Override
	public long value(int column) {
		return columns[column];
	}
	
	public void setValue(int column, long value) {
		columns[column] = value;
	}
}
