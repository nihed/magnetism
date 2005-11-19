package com.dumbhippo.web;

import java.util.List;

/**
 * Use this if you need to do anything to a list in JSTL 
 * other than iterate over it or index into it. 
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
