package com.dumbhippo.web;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.TrackView;

public class MusicGlobalPage extends AbstractSigninOptionalPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicGlobalPage.class);
	
	private MusicSystem musicSystem;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private Pageable<TrackView> recentTracks;
	private Pageable<TrackView> mostPlayedTracks;
	private Pageable<TrackView> mostPlayedToday;
	private Pageable<TrackView> onePlayTracks;
	
	public MusicGlobalPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}

	public Pageable<TrackView> getRecentTracks() {
		if (recentTracks == null) {
			recentTracks = pagePositions.createBoundedPageable("globalRecentTracks");
			musicSystem.pageLatestTrackViews(getSignin().getViewpoint(), recentTracks);
		}
		
		return recentTracks;
	}
	
	public Pageable<TrackView> getMostPlayedTracks() {
		if (mostPlayedTracks == null) {
			mostPlayedTracks = pagePositions.createBoundedPageable("globalMostPlayedTracks");
			musicSystem.pageFrequentTrackViews(getSignin().getViewpoint(), mostPlayedTracks);
		}
		return mostPlayedTracks;
	}

	public Pageable<TrackView> getMostPlayedToday() {
		if (mostPlayedToday == null) {
			// We define a "day" as 3am => 3am US/Eastern; I think its sort of fun
			// to have a point in time where the list resets to empty and
			// grows from. Alternatively, we could do last 24 hours, or
			// pay attention to where the user is located.
			
			Date dayStart = null;
			
			Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
			if (calendar.get(Calendar.HOUR_OF_DAY) < 3)
				calendar.add(Calendar.DAY_OF_YEAR, -1);
			calendar.set(Calendar.HOUR_OF_DAY, 3);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			
			dayStart = calendar.getTime();
			
			mostPlayedToday = pagePositions.createBoundedPageable("mostPlayedToday");
			musicSystem.pageFrequentTrackViewsSince(getSignin().getViewpoint(), dayStart, mostPlayedToday);
		}
		return mostPlayedToday;
	}

	public Pageable<TrackView> getOnePlayTracks() {
		if (onePlayTracks == null) {
			onePlayTracks = pagePositions.createBoundedPageable("onePlayTracks");
			musicSystem.pageOnePlayTrackViews(getSignin().getViewpoint(), onePlayTracks);
		}
		return onePlayTracks;
	}	
}
