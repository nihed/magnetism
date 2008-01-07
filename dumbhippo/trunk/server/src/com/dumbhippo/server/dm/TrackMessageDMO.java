package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.persistence.TrackMessage;
import com.dumbhippo.server.NotFoundException;

@DMO(classId="http://mugshot.org/p/o/trackMessage")
public abstract class TrackMessageDMO extends ChatMessageDMO {
	protected TrackMessageDMO(ChatMessageKey key) {
		super(key);
	}

	@Override
	protected void init() throws NotFoundException {
		message = em.find(TrackMessage.class, getKey().getId());
		if (message == null)
			throw new NotFoundException("No such track message");
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		// This is "pointless", since tracks are always visible. The reason for doing it this way
		// is simply that we want to return some visible object. (null visibility delegate means
		// "not visible", not "visible")
		
		TrackDMO track = session.findUnchecked(TrackDMO.class, ((TrackMessage)message).getTrackHistory().getTrack().getId()); 
		return track.getStoreKey();
	}
}
