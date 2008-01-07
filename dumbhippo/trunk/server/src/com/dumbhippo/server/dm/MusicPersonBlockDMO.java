package com.dumbhippo.server.dm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;

import com.dumbhippo.dm.DMFeed;
import com.dumbhippo.dm.DMFeedItem;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.blocks.MusicPersonBlockView;
import com.dumbhippo.server.views.SystemViewpoint;

@DMO(classId="http://mugshot.org/p/o/musicChatBlock")
public abstract class MusicPersonBlockDMO extends BlockDMO {
	@EJB
	private MusicSystem musicSystem;
	
	protected MusicPersonBlockDMO(BlockDMOKey key) {
		super(key);
	}
	
	@DMProperty(defaultInclude=true, defaultChildren="+", defaultMaxFetch=5)
	public DMFeed<TrackDMO> getTracks() {
		return new TracksFeed();
	}
	
	private class TracksFeed implements DMFeed<TrackDMO> {
		public Iterator<DMFeedItem<TrackDMO>> iterator(int start, int max, long minTimestamp) {
			User user = ((MusicPersonBlockView)blockView).getPersonSource().getUser();
		
			List<TrackHistory> tracks = musicSystem.getLatestTracks(SystemViewpoint.getInstance(), user, start, max, minTimestamp);
			
			List<DMFeedItem<TrackDMO>> items = new ArrayList<DMFeedItem<TrackDMO>>(); 
			for (TrackHistory track : tracks) {
				TrackDMO trackDMO = session.findUnchecked(TrackDMO.class, track.getTrack().getId());
				items.add(new DMFeedItem<TrackDMO>(trackDMO, track.getLastUpdated().getTime()));
			}
			
			return items.iterator();
		}
	}
}
