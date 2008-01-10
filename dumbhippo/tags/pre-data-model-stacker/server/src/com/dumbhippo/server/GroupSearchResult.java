package com.dumbhippo.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.Viewpoint;

/**
 * Represents the result of searching over Groups using lucene. When
 * retrieving results, those not visible to the Viewpoint passed in
 * are removed.  
 * 
 * Eventually these should be cached so that paging through the results
 * of a search doesn't require repeating the search.
 * 
 * @author otaylor
 */
public class GroupSearchResult {
	static private final Logger logger = GlobalSetup.getLogger(GroupSearchResult.class);
	
	private List<Guid> foundGroups;
	private int nChecked;
	private Hits hits;
	private String error;
	
	public GroupSearchResult(Hits hits) {
		this.hits = hits;
		foundGroups = new ArrayList<Guid>();
		nChecked = 0;
	}
	
	public GroupSearchResult(String error) {
		this.error = error;
	}
	
	/**
	 * Return an error string, if an error occurred. This is meant to be
	 * more convenient for use from a JSP than throwing an exception
	 * from GroupingBoard.searchGroups().
	 * 
	 * @return a string describing the error that occurred, or null
	 */
	public String getError() {
		return this.error;
	}
	
	/**
	 * Get an an approximate count of the number of items resulting from
	 * the search. The approximate part is that until we examine all the
	 * items, we don't know whether they are accessible to the user or not. We include
	 * all unexamined items in the count. Call this after retrieving
	 * all items you are going to display to the user, to avoid saying
	 * "Items 1 to 3 out of 5" if you are displaying the only 3 visible
	 * items.
	 * 
	 * @return the approximate number of items in the result set
	 */
	public int getApproximateCount() {
		if (this.error != null)
			return 0;
		
		return hits.length() - (nChecked - foundGroups.size());
	}
	
	
	/**
	 * Retrieve a range of group results from the search. Don't call this function,
	 * use GroupingBoard.getGroupSearchGroups() instead, which is slightly more
	 * efficient.
	 * 
	 * @param groupingBoard the groupingBoard, used to determine visibility
	 * @param viewpoint the viewpoint for the returned GroupView objects; must be the same 
	 *        as the viewpoint when initially creating the GroupSearchResult object.
	 * @param start the index of the first group to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of GroupView objects; may have less than count items when no more
	 *        are available. 
	 */
	public List<GroupView> getGroups(GroupSystem groupSystem, Viewpoint viewpoint, int start, int count) {
		if (this.error != null)
			return Collections.emptyList();
		
		// FIXME: We should save the GUID of the viewpoint and check to make sure that 
		// it is consistent here.
		
		List<GroupView> results = new ArrayList<GroupView>();
		
		int toGet = start + count - foundGroups.size();
		if (toGet < 0)
			toGet = 0;
		
		for (int i = nChecked; toGet > 0 && i < hits.length(); i++) {
			try {
				Document d = hits.doc(i);
				String id = d.get("id");
				if (id == null) {
					logger.error("Document didn't have id field");
					continue;
				}
				
				Guid guid = new Guid(id);
				Group group = groupSystem.lookupGroupById(viewpoint, guid);
				foundGroups.add(group.getGuid());
				toGet--;
				
			} catch (IOException e) { // Error reading index
			} catch (ParseException e) { // Bad ID
			} catch (NotFoundException e) { // Group not visible
			}
			
			nChecked++;
		}
		
		for (int i = start; i < foundGroups.size() && i < start + count; i++) {
			try {
				Group group = groupSystem.lookupGroupById(viewpoint, foundGroups.get(i));
				GroupView groupView = groupSystem.getGroupView(viewpoint, group);
				results.add(groupView);
			} catch (NotFoundException e) { // Shouldn't happen, ignore
			}
		}
		
		return results;
	}
}
