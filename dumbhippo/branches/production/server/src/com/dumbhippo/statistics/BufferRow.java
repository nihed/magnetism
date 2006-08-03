package com.dumbhippo.statistics;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Implementation of the Row interface for a row backed by an in-memory
 * buffer.
 * @author otaylor 
 */
class BufferRow extends Row {
	ByteBuffer buffer;
	int startOffset;
	int numColumns;
	
	public BufferRow(ByteBuffer buffer, int rowInBuffer, int numColumns) {
		this.buffer = buffer;
		this.startOffset = startOffset + rowInBuffer * (1 + numColumns) * 8;
		this.numColumns = numColumns;
	}
	
	@Override
	public Date getDate() {
		return new Date(buffer.getLong((int)startOffset));
	}

	@Override
	public int numColumns() {
		return numColumns;
	}

	@Override
	public long value(int column) {
		return buffer.getLong(startOffset + (1 + column) * 8);
	}
}
