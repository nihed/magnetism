package com.dumbhippo.statistics;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Abstract class representing a row of data stored by the statistics engine.
 * Note that this class just represents set of long data values
 * and doesn't keep track of what the data values represent. See the ColumnMap
 * class for how the contents of a row are represented.
 * @author otaylor
 */
public abstract class Row {
	/**
	 * Get the timestamp for the current row as a date object
	 * @return the timestamp
	 */
	public abstract Date getDate();
	
	/**
	 * Get the number of columns in the row 
	 * @return number of columns in the row
	 */
	public abstract int numColumns(); 

	/**
	 * Retrieve the long value from the particular column
	 * @param column column to retrieve
	 * @return value of the given column
	 */
	public abstract long value(int column);
	
	/**
	 * Helper function to serialize the contents of the row to bytes
	 * @return a byte array containing the contents of the row. The
	 *   date is serialized first as a 64-bit value followed by columns
	 *   in order, also as 64-bit values.
	 */
	public byte[] toByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate((1 + numColumns()) * 8);
		Date date = getDate();
		if (date != null)
			buffer.putLong(date.getTime());
		else
			buffer.putLong(0);
		for (int i = 0; i < numColumns(); i++)
			buffer.putLong(value(i));
		
		return buffer.array();
	}
}
