package com.dumbhippo.search;

import javax.ejb.Local;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Track;

@Local
public interface SearchSystem {
	/**
	 * Asynchronously index or reindex the given post after the end of the
	 * current transaction.
	 *  
	 * @param post the post to index.
	 * @param reindex true if the item already exists and needs to be reindexed.
	 *   (Posts are currently never modified, so this option is mostly provided 
	 *   as a matter of completeness)  
	 */
	void indexPost(Post post, boolean reindex);

	/**
	 * Asynchronously index or reindex the given group after the end of the
	 * current transaction.
	 *  
	 * @param group the group to index.
	 * @param reindex true if the item already exists and needs to be reindexed
	 */
	void indexGroup(Group group, boolean reindex);

	/**
	 * Asynchronously index the given track after the end of the
	 * current transaction. (There is no reindex parameter since Tracks have
	 * no identity other than the Artist/Album/Song triple, so are conceptually
	 * immutable.)
	 *  
	 * @param track the track to index.
	 */
	void indexTrack(Track track);
	
	/**
	 * Asynchronously reindex all indexed items
	 */
	void reindexAll();
}
