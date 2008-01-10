package com.dumbhippo.dm.store;

/**
 * This class maintains a log that indicates the (minimum) item timestamp of
 * an entry in a feed that changed for each transaction timestamp. This can
 * be used to determine what portion of a feed needs to be resent to a consumer
 * of the feed, if we know the txTimestamp of the transaction that was last
 * used to update them on the feed state.
 * 
 * Note that because the way txTimestamps work the resending is conservative: we
 * can tell what items might have changed after the start of the given transaction, 
 * but we can't tell if they actually did or not.
 * 
 * An itemTimestamp of -1 in the log has a special significance: it means that
 * the feed changed in some other way than inserting or restacking a feed item,
 * so the entire feed will need to be resent from scratch. 
 * 
 * @author otaylor
 */
public class FeedLog {
	private static final int INITIALIZE_SIZE = 10;

	private long txTimestamps[];
	private long itemTimestamps[];
	private int entryCount = 0;
	
	public synchronized void addEntry(long txTimestamp, long itemTimestamp) {
		int insertionPos = entryCount;
		while (insertionPos > 0 && txTimestamps[insertionPos - 1] >= txTimestamp)
			insertionPos--;
		
		/* If the txTimestamp is the same as one already in the log, we just replace
		 * the item timestamp for that log entry.
		 */
		if (insertionPos < entryCount && txTimestamps[insertionPos] == txTimestamp) {
			if (itemTimestamps[insertionPos] > itemTimestamp)
				itemTimestamps[insertionPos] = itemTimestamp;
			return;
		}
		
		if (entryCount == 0) {
			txTimestamps = new long[INITIALIZE_SIZE];
			itemTimestamps = new long[INITIALIZE_SIZE];
		} else if (entryCount == txTimestamps.length) {
			int newSize = entryCount * 2;
			if (newSize < 0)
				throw new OutOfMemoryError();
			
			long newTxTimestamps[] = new long[newSize];
			System.arraycopy(txTimestamps, 0, newTxTimestamps, 0, entryCount);
			txTimestamps = newTxTimestamps;
			
			long newItemTimestamps[] = new long[newSize];
			System.arraycopy(itemTimestamps, 0, newItemTimestamps, 0, entryCount);
			itemTimestamps = newItemTimestamps;
		}
		
		System.arraycopy(txTimestamps, insertionPos, txTimestamps, insertionPos + 1, entryCount - insertionPos);
		System.arraycopy(itemTimestamps, insertionPos, itemTimestamps, insertionPos + 1, entryCount - insertionPos);
		entryCount++;
		
		txTimestamps[insertionPos] = txTimestamp;
		itemTimestamps[insertionPos] = itemTimestamp;
	}
	
	public synchronized long getMinItemTimestamp(long txTimestamp) {
		long minTimestamp = Long.MAX_VALUE;
		
		for (int i = entryCount - 1; i >= 0; i--) {
			if (txTimestamps[i] < txTimestamp)
				break;
			
			if (itemTimestamps[i] < minTimestamp)
				minTimestamp = itemTimestamps[i];
		}
		
		return minTimestamp;
	}
}
