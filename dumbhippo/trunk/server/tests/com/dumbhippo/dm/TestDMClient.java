package com.dumbhippo.dm;

import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.StoreClient;
import com.dumbhippo.identity20.Guid;

public class TestDMClient implements DMClient {
	private Guid viewerId;
	private StoreClient storeClient;
	private FetchResult lastNotification;
	private long lastSeenSerial = -1;

	public TestDMClient(DataModel model, Guid viewerId) {
		this.viewerId = viewerId;
		
		storeClient = model.getStore().openClient(this);
	}

	public DMViewpoint createViewpoint() {
		return new TestViewpoint(viewerId);
	}
	
	public FetchResult getLastNotification() {
		return lastNotification;
	}

	public StoreClient getStoreClient() {
		return storeClient;
	}

	public FetchVisitor beginNotification() {
		return new FetchResultVisitor(this);
	}

	public void endNotification(FetchVisitor visitor, long serial) {
		FetchResultVisitor resultVisitor = (FetchResultVisitor)visitor;
		
		if (serial > lastSeenSerial) {
			lastNotification = resultVisitor.getResult();
			lastSeenSerial = serial;
		}
	}

	public <K, T extends DMObject<K>> void notifyEviction(DMClassHolder<K,T> classHolder, K key, long serial) {
	}

	public void nullNotification(long serial) {
	}
	
	@Override
	public String toString() {
		return "TestDMClient(" + viewerId + ")"; 
	}
}
