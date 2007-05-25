package com.dumbhippo.dm;

import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;

/**
 * The client is responsible for delivering the notifications in the order of the 
 * serials. There will be no gaps in the serial sequence, so notifications can be
 * queued until the next notification comes in (that's what the nullNotification
 * is for, marking a notification that got cancelled). The first serial for
 * any client will be serial 0.
 *
 * When the client is spontaneously fetching data, it must allocate a serial
 * using StoreClient.allocateSerial() before the fetch and treat the data it
 * receives in the FetchVisitor as if it was a notification delivered with
 * that serial.
 * 
 * @author otaylor
 */
public interface DMClient {
	/**
	 * Start assembling a notification packet. Notifications will be added to the
	 * packet via the returned FetchVisitor. When the notification is complete,
	 * endNotification will be called.
	 */
	FetchVisitor beginNotification();
	
	DMViewpoint getViewpoint();
	
	void endNotification(FetchVisitor visitor, long serial);
	
	<K,T extends DMObject<K>> void notifyEviction(DMClassHolder<T> classHolder, K key, long serial);
	
	void nullNotification(long serial);
}
