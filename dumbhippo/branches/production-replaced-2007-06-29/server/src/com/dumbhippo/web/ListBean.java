package com.dumbhippo.web;

import java.util.List;

/**
 * DEPRECATED: This class is deprecated; instead of using this class,
 * simply call the fn:length method from the JSP function library
 * to determine list length.  It's not worth putting @Deprecated
 * on the class yet until we get manageably few warnings.
 * 
 * This is a wrapper for List that adds bean-style getters/setters,
 * right now it's just a way to rename size() to getSize().
 * This allows you to access the list from JSTL.
 * 
 * Don't try to add an isEmpty() property because "empty" 
 * is a keyword in JSTL
 * 
 * @author hp
 */
public class ListBean<T> {

	private List<T> list;
	
	public ListBean(List<T> list) {
		this.list = list;
	}
	
	public int getSize() {
		return list.size();
	}
	
	public List<T> getList() {
		return list;
	}
}
