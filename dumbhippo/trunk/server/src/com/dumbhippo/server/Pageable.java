package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
*  The fields you need to set if you create a pageable are:
 *   name
 * 
 * Optional fields to set if you create a pageable, if you don't like the defaults:
 *   position (defaults to 0) 
 *   initialPerPage (defaults to 3)
 *   subsequentPerPage (defaults to 6)
 *   bound (defaults to unbounded)
 *   flexibleResultCount (defaults to false)
 *  
 * Fields you need to set if you fill in a pageable are:
 *   results
 *   totalCount
 */
public class Pageable<T> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(Pageable.class);
	static final int DEFAULT_INITIAL_PER_PAGE = 3;
	static final int DEFAULT_SUBSEQUENT_PER_PAGE = 6;
	
	private String name;
	private List<T> results;
	private int totalCount;
	private int position;
	private int initialPerPage;
	private int subsequentPerPage;
	private int bound;
	private boolean flexibleResultCount;
	
	public Pageable(String name) {
		this.name = name;
		this.totalCount = -1;
		this.bound = -1;
		this.flexibleResultCount = false;
		initialPerPage = DEFAULT_INITIAL_PER_PAGE;
		subsequentPerPage = DEFAULT_SUBSEQUENT_PER_PAGE;
	}
	
	/** 
	 * The pageable has a name used as a basis for anchors in the jsp. The name should be unique for each pageable on a jsp.
	 * The method filling in the pageable should not look at this or care about it.
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/** 
	 * The number of items to show on the first page of results. Set by the creator
	 * of the pageable and honored by the method that fills in the pageable.
	 * @return
	 */
	public int getInitialPerPage() {
		return initialPerPage;
	}
	public void setInitialPerPage(int initialPerPage) {
		this.initialPerPage = initialPerPage;
	}

	/** 
	 * The number of items to show on each page of results after the first. Set by the creator
	 * of the pageable and honored by the method that fills in the pageable.
	 * @return
	 */
	public int getSubsequentPerPage() {
		return subsequentPerPage;
	}
	public void setSubsequentPerPage(int subsequentPerPage) {
		this.subsequentPerPage = subsequentPerPage;
	}
	
	/** Gets the items on the current page. These are provided by the method 
	 * that fills in the pageable.
	 * @return
	 */
	public List<T> getResults() {
		return results;
	}
	
	/** Get the number of items on the current page.
	 * 
	 * @return
	 */
	public int getResultCount() {
		if (results != null)
			return results.size();
		else
			return 0;
	}	
	
	/* The number of available pages; set by the method that fills in the 
	 * pageable.
	 */
	public int getTotalCount() {
		return totalCount;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
		// if the total page count is actually less than the "requested" position,
		// this will readjust the position
	    position = Math.min(position, getPageCount() - 1);
	}

	public void setResults(List<T> results) {
		this.results = results;
	}
	
	/** The current page; set by the code that creates the pageable, and honored
	 * by the method that fills in the pageable.
	 * @return
	 */
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	/** 
	 * An arbitrary limit set by the creator of the pageable and honored by the method that
	 * fills in the results. This is used when the number of items could be "infinite" or 
	 * "huge" and we want to just arbitrarily put a cap on how many pages you can look at.
	 * The bound is the number of items, not the number of pages.
	 * 
	 * @return
	 */
	public int getBound() {
		return bound;
	}
	
	public void setBound(int bound) {
		this.bound = bound;
	}

	/** 
	 * Set by the creator of the pageable, we use this to decide not to show the total count of items
	 * available next to the More... link. FIXME someone fix these docs to explain better what this 
	 * means conceptually. 
	 * @return
	 */
	public boolean isFlexibleResultCount() {
		return flexibleResultCount;
	}
	
	public void setFlexibleResultCount(boolean flexibleResultCount) {
		this.flexibleResultCount = flexibleResultCount;
	}
	
	/**
	 * Returns the requested first item index. The creator of the pageable specifies this implicitly 
	 * by providing the position (current page) and the size of each page. The method filling 
	 * in the pageable would look at the start index to decide where to begin returning results.
	 * @return
	 */
	public int getStart() {
		return position == 0 ? 0 : initialPerPage + (position - 1) * subsequentPerPage;
	}

	/**
	 * Returns the requested number of results to be filled in, which will be either 
	 * the requested initial per-page count or the requested subsequent per-page count
	 * depending on our page position.
	 * 
	 * @return
	 */
	public int getCount() {
		return position == 0 ? initialPerPage : subsequentPerPage;
	}
	
	/** 
	 * Get the total number of pages available; set by the method that fills in the 
	 * pageable.
	 * @return
	 */
	public int getPageCount() {
		int itemCount = totalCount;
		if (bound >= 0 && itemCount > bound)
			itemCount = bound;
		
		if (itemCount < initialPerPage) {
			return 1;
		} else {
			return 1 + (itemCount - initialPerPage + subsequentPerPage - 1) / subsequentPerPage;
		}
	}
	
	/**
	 * Return the actual number of items on the current page. Valid only after filling in the 
	 * pageable, because it uses getTotalCount() in the calculation.
	 * 
	 * @return
	 */
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
	 * Sets results for a page to be an appropriate subset of allItems. That is, if 
	 * allItems is all available items then this call setResults() with the subset of 
	 * the list that would be on the current page.
	 * 
	 * Also does a setTotalCount() to the size of allItems.
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
	
	/**
	 * Modifies the passed-in list to contain only the requested single page of results, 
	 * by deleting items that would be on previous and subsequent pages.
	 * 
	 * @param allItems
	 */
	public void chopForPageResults(List<?> allItems) {
		int i = Math.min(allItems.size(), getStart());
		int count = Math.min(allItems.size() - getStart(), getCount());
		allItems.subList(0, i).clear();
		allItems.subList(count, allItems.size()).clear();
	}
}
