package com.dumbhippo.server.dm;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.annotations.DMInit;
import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.services.LastFmWebServices;

// We directly put the database ID into the resourceId, which is is perhaps
// a little questionable, since we generally try to keep the database IDs
// from DBUnique objects out of our externally exported protoocol and URLs.
//
// The alternative would be to munge the artist/name into a string ID, but
// then it would no longer correspond 1:1 to a Track object, which would
// make our life a lot more complicated. 
//
// The way we build the  resourceId doesn't have to be stable between server 
// restarts, so we can always change it later.

@DMO(classId="http://mugshot.org/p/o/track", resourceBase="/o/track")
public abstract class TrackDMO extends DMObject<Long> {
	private Track track;
	private TrackView trackView;
	
	private static final int TRACK_VIEW_GROUP = 1;
	
	@Inject
	private EntityManager em;
	
	@EJB
	private MusicSystem musicSystem;
	
	protected TrackDMO(Long key) {
		super(key);
	}
	
	@Override
	protected void init() throws NotFoundException {
		track = em.find(Track.class, getKey());
		if (track == null)
			throw new NotFoundException("No such track");
	}
	
	@DMProperty(defaultInclude=true)
	public String getName() {
		return track.getName();
	}
	
	@DMProperty(defaultInclude=true)
	public String getArtist() {
		return track.getArtist();
	}
	
	@DMProperty(defaultInclude=true, type=PropertyType.URL)
	public String getLink() {
		if (getArtist() != null && getName() != null) {
			return LastFmWebServices.makeWebLink(getArtist(), getName());
		} else {
			return null;
		}
	}

	@DMInit(group=TRACK_VIEW_GROUP)
	public void initTrackView() {
		trackView = musicSystem.getTrackView(track);
	}
	
	@DMProperty(defaultInclude=true, group=TRACK_VIEW_GROUP)
	public int getImageWidth() {
		return trackView.getSmallImageWidth();
	}

	@DMProperty(defaultInclude=true, group=TRACK_VIEW_GROUP)
	public int getImageHeight() {
		return trackView.getSmallImageHeight();
	}

	@DMProperty(type=PropertyType.URL, defaultInclude=true, group=TRACK_VIEW_GROUP)
	public String getImageUrl() {
		return trackView.getSmallImageUrl();
	}
	
	@DMProperty(defaultInclude=true, group=TRACK_VIEW_GROUP)
	public long getDuration() {
		return trackView.getDurationSeconds() * 1000;
	}
}
