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
import com.dumbhippo.persistence.Post;

/**
 * Represents the result of searching over Posts using lucene. When
 * retrieving results, those not visible to the Viewpoint passed in
 * are removed.  
 * 
 * Eventually these should be cached so that paging through the results
 * of a search doesn't require repeating the search.
 * 
 * @author otaylor
 */
public class PostSearchResult {
	static private final Logger logger = GlobalSetup.getLogger(PostSearchResult.class);
	
	private List<Guid> foundPosts;
	private int nChecked;
	private Hits hits;
	private String error;
	
	public PostSearchResult(Hits hits) {
		this.hits = hits;
		foundPosts = new ArrayList<Guid>();
		nChecked = 0;
	}
	
	public PostSearchResult(String error) {
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
		
		return hits.length() - (nChecked - foundPosts.size());
	}
	
	
	/**
	 * Retrieve a range of post results from the search. Don't call this function,
	 * use PostingBoard.getPostSearchPosts() instead, which is slightly more
	 * efficient.
	 * 
	 * @param postingBoard the postingBoard, used to determine visibility
	 * @param viewpoint the viewpoint for the returned PostView objects; must be the same 
	 *        as the viewpoint when initially creating the PostSearchResult object.
	 * @param start the index of the first post to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of PostView objects; may have less than count items when no more
	 *        are available. 
	 */
	public List<PostView> getPosts(PostingBoard postingBoard, Viewpoint viewpoint, int start, int count) {
		if (this.error != null)
			return Collections.emptyList();
		
		// FIXME: We should save the GUID of the viewpoint and check to make sure that 
		// it is consistent here.
		
		List<PostView> results = new ArrayList<PostView>();
		
		int toGet = start + count - foundPosts.size();
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
				Post post = postingBoard.loadRawPost(viewpoint, guid);
				// Special case ignoring these; the fact that the
				// person inviting created a post is basically
				// an implementation detail
				if (postingBoard.postIsGroupNotification(post))
					continue;
				foundPosts.add(post.getGuid());
				toGet--;
				
			} catch (IOException e) { // Error reading index
			} catch (ParseException e) { // Bad ID
			} catch (NotFoundException e) { // Post not visible
			}
			
			nChecked++;
		}
		
		for (int i = start; i < foundPosts.size() && i < start + count; i++) {
			try {
				PostView postView = postingBoard.loadPost(viewpoint, foundPosts.get(i));
				results.add(postView);
			} catch (NotFoundException e) { // Shouldn't happen, ignore
			}
		}
		
		return results;
	}
}
