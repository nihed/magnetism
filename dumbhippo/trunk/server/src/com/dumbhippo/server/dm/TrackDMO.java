package com.dumbhippo.server.dm;

import javax.ejb.EJB;
import javax.persistence.EntityManager;

import com.dumbhippo.dm.DMObject;
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
	
	// Possible improvement:
	// 
	// If we do anything that requires getting the trackView, we really want to make 
	// sure that *all* of the work that we did to fetch the cover art, the download
	// links, etc, gets saved into the DM cache. That would probably require improvements
	// to the engine ... maybe something like @DMProperty(group=1) to mark a number
	// of properties that should be cached together.
	
	@DMProperty(defaultInclude=true)
	public int getImageWidth() {
		ensureTrackView();
		
		return trackView.getSmallImageWidth();
	}

	@DMProperty(defaultInclude=true)
	public int getImageHeight() {
		ensureTrackView();
		
		return trackView.getSmallImageHeight();
	}

	@DMProperty(type=PropertyType.URL, defaultInclude=true)
	public String getImageUrl() {
		ensureTrackView();
		
		return trackView.getSmallImageUrl();
	}
	
	@DMProperty(defaultInclude=true)
	public long getDuration() {
		ensureTrackView();
		
		return trackView.getDurationSeconds() * 1000;
	}
		
	private void ensureTrackView() {
		trackView = musicSystem.getTrackView(track);
	}
}
