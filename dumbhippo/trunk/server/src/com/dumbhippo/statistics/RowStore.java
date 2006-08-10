package com.dumbhippo.statistics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class manages the low-level details of reading and writing from a
 * statistics data file. Disk acess is done by memory mapping. The primary
 * advantage of memory mapping is not really efficiency; since we are either
 * appending rows to the end of the file (writing) or scanning through it 
 * sequentially (reading), but rather memory mapping makes things easier to
 * manage when we are appending to the file and also retrieving data from it
 * from different threads.
 * @author otaylor
 */
public class RowStore {
	static final int BLOCK_ROWS = 8192;
	
	FileChannel channel;
	boolean readWrite;
	long startOffset;
	int numColumns;
	long numRows;
	Block appendBlock;
	List<Block> blocks = new ArrayList<Block>();  

	private RowStore(FileChannel channel, long startOffset, int numColumns, long numRows, boolean readWrite) {
		this.channel = channel;
		this.startOffset = startOffset;
		this.numColumns = numColumns;
		this.numRows = numRows;
		this.readWrite = readWrite;
	}
	
	static public RowStore createReadOnly(FileChannel channel, long startOffset, int numColumns, long numRows) {
		return new RowStore(channel, startOffset, numColumns, numRows, false);
	}
	
	static public RowStore createReadWrite(FileChannel channel, long startOffset, int numColumns, long numRows) {
		return new RowStore(channel, startOffset, numColumns, numRows, true);
	}
	
	public void appendRow(Row row) {
		if (!readWrite)
			throw new RuntimeException("appendRow() called on a read-only RowStore");
		if (appendBlock == null)
			appendBlock = lockBlock(numRows, true);
		
		byte[] bytes = row.toByteArray();
		appendBlock.buffer.position((int)(numRows - appendBlock.startRow) * bytes.length);
		appendBlock.buffer.put(bytes);
		
		numRows++;
		if (numRows >= appendBlock.startRow + BLOCK_ROWS) {
			unlockBlock(appendBlock);
			appendBlock = null;
		}
	}
	
	public RowIterator getIterator(long startRow, long endRow) {
		return new RowStoreIterator(startRow, endRow);
	}
	
	static class Block {
		ByteBuffer buffer;
		long startRow;
		int lockCount;
		boolean readWrite;
	}
	
	Block lockBlock(long row, boolean readWrite) {
		int blockIndex = (int)(row / BLOCK_ROWS);
		
		synchronized(blocks) {
			if (blockIndex < blocks.size()) {
				if (readWrite)
					throw new RuntimeException("Attempt to lock an existing block for writing");
				
				Block existingBlock = blocks.get(blockIndex);
				if (existingBlock != null) {
					existingBlock.lockCount++;
					return existingBlock;
				}
			}
			
			Block newBlock = new Block();
			newBlock.startRow = blockIndex * BLOCK_ROWS;
			newBlock.lockCount = 1;
			newBlock.readWrite = readWrite;
			
			long blockSize = (1 + numColumns) * 8 * BLOCK_ROWS;
			long blockStart = startOffset + blockIndex * blockSize; 
			
			try {
				newBlock.buffer = channel.map(readWrite ? MapMode.READ_WRITE : MapMode.READ_ONLY, blockStart, blockSize);
			} catch (IOException e) {
				throw new RuntimeException("Error mapping row store block");
			}
			
			for (int i = blocks.size(); i <= blockIndex; i++)
				blocks.add(null);
			
			blocks.set(blockIndex, newBlock);
			
			return newBlock;
		}
	}
	
	void unlockBlock(Block block) {
		synchronized(blocks) {
			block.lockCount--;
			if (block.lockCount == 0 && block.readWrite) {
				int blockIndex = (int)(block.startRow / BLOCK_ROWS);
				blocks.set(blockIndex, null);
			}
		}		
	}
	
	private class RowStoreIterator implements RowIterator {
		long startIndex;
		long endIndex;
		long nextIndex;
		Row nextRow;
		Block nextRowBlock;
		
		public RowStoreIterator(long startRow, long endRow) {
			this.startIndex = startRow;
			if (endRow > numRows) {
				// don't try to iterate through rows that are not there
				this.endIndex = numRows;
			} else {
			    this.endIndex = endRow;
			}
			nextIndex = startRow;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		public boolean hasNext() {
			getNextRow(false);
			return nextIndex < endIndex;
		}
		
		// only advances nextRow if nextRow is null
		private Row getNextRow(boolean mustHaveNext) {
			if (mustHaveNext && (nextIndex == endIndex))
				throw new NoSuchElementException();

			// keep advancing nextIndex and nextRow if nextRow time is 0 (i.e. nextRow is a placeholder)
			while (((nextRow == null) || (nextRow.getDate().getTime() == 0)) && (nextIndex < endIndex)) {
				if (nextRowBlock == null)
					nextRowBlock = lockBlock(nextIndex, false);
								
				nextRow = new BufferRow(nextRowBlock.buffer, (int)(nextIndex - nextRowBlock.startRow), numColumns);		
				
				nextIndex++;

				// if we won't need nextRowBlock anymore, unlock it
				if (nextIndex >= nextRowBlock.startRow + BLOCK_ROWS || nextIndex == endIndex) {
					unlockBlock(nextRowBlock);
				}
			}
			
			return nextRow;
		}
		
		public Date nextDate() {
			return getNextRow(true).getDate(); 
		}
		
		public Row next() {
			Row result = getNextRow(true);
			// set nextRow to null so that it is advanced next time getNextRow() is called
			nextRow = null;
			
			return result;
		}
		
		@Override
		protected void finalize() {
			if (nextRowBlock != null)
				unlockBlock(nextRowBlock);
		}
	}	
}
