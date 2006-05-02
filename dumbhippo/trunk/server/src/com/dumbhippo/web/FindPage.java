package com.dumbhippo.web;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PostSearchResult;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * Backing bean for find.jsp ... as find.jsp is a temporary name for a 
 * search.jsp replacement, this is a temporary name for a SearchPage 
 * replacement.
 * 
 * @author otaylor
 */
public class FindPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FindPage.class);
	
	// We override the default values from Pageable
	static private final int INITIAL_PER_PAGE = 3;
	static private final int SUBSEQUENT_PER_PAGE = 10;
	
	@Signin
	private SigninBean signin;
	
	@PagePositions
	private PagePositionsBean pagePositions;

	private PostingBoard postBoard;
	
	private String searchText;

	private PostSearchResult searchResult;
	private Pageable<PostView> posts;
	
	public FindPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	private void ensureSearchResult() {
		if (searchResult == null) {
			searchResult = postBoard.searchPosts(signin.getViewpoint(), searchText);
			posts = pagePositions.createPageable("posts");
			posts.setInitialPerPage(INITIAL_PER_PAGE);
			posts.setSubsequentPerPage(SUBSEQUENT_PER_PAGE);

			List<PostView> resultList = postBoard.getPostSearchPosts(signin.getViewpoint(), searchResult, posts.getStart(), posts.getCount());
			
			posts.setTotalCount(searchResult.getApproximateCount());
			posts.setResults(resultList);
		}
	}
	
	public String getError() {
		ensureSearchResult();

		return searchResult.getError();
	}
	
	public int getTotal() {
		ensureSearchResult();

		return searchResult.getApproximateCount();
	}
	
	public Pageable<PostView> getPosts() {
		ensureSearchResult();
		
		return posts;
	}

	public SigninBean getSignin() {
		return signin;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		if (searchText != null)
			searchText = searchText.trim();
		this.searchText = searchText;
	}
}
