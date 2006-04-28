package com.dumbhippo.web;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.TrackView;

public class MusicGlobalPage extends AbstractSigninOptionalPage {
	
	static private final int MAX_RESULTS = 3;
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicGlobalPage.class);
	
	private MusicSystem musicSystem;
	
	private ListBean<TrackView> recentTracks;
	private ListBean<TrackView> mostPlayedTracks;
	private ListBean<TrackView> mostPlayedToday;
	private ListBean<TrackView> onePlayTracks;
	
	public MusicGlobalPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}

	public ListBean<TrackView> getRecentTracks() {
		if (recentTracks == null) {
			recentTracks = new ListBean<TrackView>(musicSystem.getPopularTrackViews(MAX_RESULTS));
		}
		
		return recentTracks;
	}
	
	public ListBean<TrackView> getMostPlayedTracks() {
		if (mostPlayedTracks == null) {
			mostPlayedTracks = new ListBean<TrackView>(musicSystem.getFrequentTrackViews(getSignin().getViewpoint(), MAX_RESULTS));
		}
		return mostPlayedTracks;
	}

	public ListBean<TrackView> getMostPlayedToday() {
		if (mostPlayedToday == null) {
			// We define a "day" as 3am => 3am US/Eastern; I think its sort of fun
			// to have a point in time where the list resets to empty and
			// grows from. Alternatively, we could do last 24 hours, or
			// pay attention to where the user is located.
			
			Date dayStart = null;
			
			Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
			if (calendar.get(Calendar.HOUR) < 3)
				calendar.add(Calendar.DAY_OF_YEAR, -1);
			calendar.set(Calendar.HOUR_OF_DAY, 3);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			
			dayStart = calendar.getTime();
			
			logger.debug("Day start was {} hours ago", (System.currentTimeMillis() - dayStart.getTime()) / (60 * 60 * 1000));
		
			mostPlayedToday = new ListBean<TrackView>(musicSystem.getFrequentTrackViewsSince(getSignin().getViewpoint(), dayStart, MAX_RESULTS));
		}
		return mostPlayedToday;
	}

	public ListBean<TrackView> getOnePlayTracks() {
		if (onePlayTracks == null) {
			onePlayTracks = new ListBean<TrackView>(musicSystem.getOnePlayTrackViews(getSignin().getViewpoint(), MAX_RESULTS));
		}
		return onePlayTracks;
	}	
}
