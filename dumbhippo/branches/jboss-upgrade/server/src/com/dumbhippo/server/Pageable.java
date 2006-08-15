package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

public class Pageable<T> {
	static final int DEFAULT_INITIAL_PER_PAGE = 3;
	static final int DEFAULT_SUBSEQUENT_PER_PAGE = 6;
	
	private String name;
	private List<T> results;
	private int totalCount;
	private int position;
	private int initialPerPage;
	private int subsequentPerPage;
	private int bound;
	
	public Pageable(String name) {
		this.name = name;
		this.totalCount = -1;
		this.bound = -1;
		initialPerPage = DEFAULT_INITIAL_PER_PAGE;
		subsequentPerPage = DEFAULT_SUBSEQUENT_PER_PAGE;
	}
	
	public String getName() {
		return name;
	}
	
	public int getInitialPerPage() {
		return initialPerPage;
	}
	public void setInitialPerPage(int initialPerPage) {
		this.initialPerPage = initialPerPage;
	}
	
	public int getSubsequentPerPage() {
		return subsequentPerPage;
	}
	public void setSubsequentPerPage(int subsequentPerPage) {
		this.subsequentPerPage = subsequentPerPage;
	}
	
	public List<T> getResults() {
		return results;
	}
	
	public int getResultCount() {
		if (results != null)
			return results.size();
		else
			return 0;
	}	
	
	public int getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public void setResults(List<T> results) {
		this.results = results;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public int getBound() {
		return bound;
	}
	
	public void setBound(int bound) {
		this.bound = bound;
	}
	
	public int getStart() {
		return position == 0 ? 0 : initialPerPage + (position - 1) * subsequentPerPage;
	}
	
	public int getCount() {
		return position == 0 ? initialPerPage : subsequentPerPage;
	}
	
	public int getPageCount() {
		int itemCount = totalCount;
		if (bound >= 0 && itemCount > bound)
			itemCount = bound;
		
		if (itemCount < initialPerPage)
			return 1;
		else
			return 1 + (itemCount - initialPerPage + subsequentPerPage - 1) / subsequentPerPage;
	}
	
	public int getCurrentItemCount() {
		int pageCount = getPageCount();
		if (position == 0) {
			return Math.min(initialPerPage, totalCount);
		} else if (position == (pageCount - 1)) {
			return totalCount - (initialPerPage + (pageCount-1) * subsequentPerPage); 
		} else {
			return subsequentPerPage;
		}
	}
	
	/**
	 * Sets results for a page to be an appropriate subset of allItems.
	 * 
	 * @param allItems a list of items that are being paged
	 */
	public void generatePageResults(List<T> allItems) {
		setTotalCount(allItems.size());				
		List<T> pageResults = new ArrayList<T>();
		int i = Math.min(allItems.size(), getStart());
		int count = Math.min(allItems.size() - getStart(), getCount());
		while (count > 0) {
			pageResults.add(allItems.get(i));
			--count;
			++i;
		}
		setResults(pageResults);
	}
	
	public void chopForPageResults(List<?> allItems) {
		int i = Math.min(allItems.size(), getStart());
		int count = Math.min(allItems.size() - getStart(), getCount());
		allItems.subList(0, i).clear();
		allItems.subList(count, allItems.size()).clear();
	}
}
