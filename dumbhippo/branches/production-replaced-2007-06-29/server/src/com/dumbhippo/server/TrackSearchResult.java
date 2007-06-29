package com.dumbhippo.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Represents the result of searching over Track using lucene.
 * 
 * Eventually these should be cached so that paging through the results
 * of a search doesn't require repeating the search.
 * 
 * @author otaylor
 */
public class TrackSearchResult {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(TrackSearchResult.class);
	
	Hits hits;
	String error;
	
	public TrackSearchResult(Hits hits) {
		this.hits = hits;
	}
	
	public TrackSearchResult(String error) {
		this.error = error;
	}
	
	/**
	 * Return an error string, if an error occurred. This is meant to be
	 * more convenient for use from a JSP than throwing an exception
	 * from PostingBoard.searchPosts().
	 * 
	 * @return a string describing the error that occurred, or null
	 */
	public String getError() {
		return this.error;
	}
	
	/**
	 * Get an an approximate count of the number of items resulting from
	 * the search. (This is exact at the moment, but we might have 
	 * visibility or retrieval failure issues in the future.) Call this 
	 * after retrieving all items you are going to display to the user, 
	 * to avoid saying
	 * "Items 1 to 3 out of 5" if you are displaying the only 3 visible
	 * items.
	 * 
	 * @return the approximate number of items in the result set
	 */
	public int getApproximateCount() {
		if (this.error != null)
			return 0;
		
		return hits.length();
	}
	
	
	/**
	 * Retrieve a range of track results from the search. Don't call this function,
	 * use MusicSystem.getTrackSearchTracks() instead, which is slightly more
	 * efficient.
	 * 
	 * @param musicSystemInternal used to retrieve TrackViews
	 * @param viewpoint the viewpoint for the returned TrackView objects; must be the same 
	 *        as the viewpoint when initially creating the TrackSearchResult object.
	 * @param start the index of the first track to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of TrackView objects; may have less than count items when no more
	 *        are available. 
	 */
	@SuppressWarnings("unused") 	
	public List<TrackView> getTracks(MusicSystem musicSystem, Viewpoint viewpoint, int start, int count) {
		if (this.error != null)
			return Collections.emptyList();
		
		// FIXME: We should save the GUID of the viewpoint and check to make sure that 
		// it is consistent here.
		
		List<TrackView> results = new ArrayList<TrackView>(count);
		
		for (int i = start;  i < start + count && i < hits.length(); i++) {
			try {
				Document d = hits.doc(i);
				String artist = d.get("artist");
				String album = d.get("album");
				String name = d.get("name");

				TrackView trackView = musicSystem.songSearch(viewpoint, artist, album, name);
				results.add(trackView);
			} catch (NotFoundException e) {
				// Ignore this item
			} catch (IOException e) {
				// From Hibernate, ignore this item
			}			
		}
		
		return results;
	}
}
