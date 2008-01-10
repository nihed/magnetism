package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.server.blocks.MusicChatBlockView;

@DMO(classId="http://mugshot.org/p/o/musicPersonBlock")
public abstract class MusicChatBlockDMO extends BlockDMO {
	protected MusicChatBlockDMO(BlockDMOKey key) {
		super(key);
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+")
	public TrackDMO getTrack() {
		return session.findUnchecked(TrackDMO.class, ((MusicChatBlockView)blockView).getTrack().getTrackHistory().getTrack().getId());
	}
	
	@DMProperty(defaultInclude=true)
	public long getTrackPlayTime() {
		return ((MusicChatBlockView)blockView).getTrack().getTrackHistory().getLastUpdated().getTime();
	}
}
