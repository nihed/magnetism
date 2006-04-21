package com.dumbhippo.web;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
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
	
	static private final int DEFAULT_COUNT = 15;
	static private final int MAX_COUNT = 50;
	
	@Signin
	private SigninBean signin;

	private PostingBoard postBoard;
	
	private String searchText;
	private int start;
	private int count;

	private PostSearchResult searchResult;
	private ListBean<PostView> posts;

	
	public FindPage() {
		searchText = "";
		start = 0;
		count = DEFAULT_COUNT;
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}
	
	private void ensureSearchResult() {
		if (searchResult == null) {
			searchResult = postBoard.searchPosts(signin.getViewpoint(), searchText);
			List<PostView> resultList = postBoard.getPostSearchPosts(signin.getViewpoint(), searchResult, start, count);
			posts = new ListBean<PostView>(resultList);
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
	
	public ListBean<PostView> getPosts() {
		ensureSearchResult();
		
		return posts;
	}
	
	private String getParams(int start) {
		if (start < 0)
			start = 0;
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("q=");
		sb.append(StringUtils.urlEncode(searchText));
		sb.append("&start=" + start);
		sb.append("&count=" + getCount());
		
		
		return sb.toString();
	}
	
	public String getPreviousParams() {
		return getParams(getStart() - getCount());
	}
	
	public String getNextParams() {
		return getParams(getStart() + getCount());
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		if (count > MAX_COUNT || count < 1)
			this.count = MAX_COUNT;
		else
			this.count = count;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		if (searchText != null)
			searchText = searchText.trim();
		this.searchText = searchText;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		if (start < 0)
			start = 0;
		this.start = start;
	}
	
	public int getEnd() {
		ensureSearchResult();

		if (count <= posts.getSize())
			return start + count - 1;
		else
			return start + posts.getSize() - 1;
	}
}
