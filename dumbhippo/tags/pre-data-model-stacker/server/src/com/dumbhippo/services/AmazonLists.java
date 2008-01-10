package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AmazonLists implements AmazonListsView {
	
	private int total;
	private List<AmazonListView> lists;
	
	public AmazonLists() {
		this(-1);
	}
	
	public AmazonLists(int total) {		
		this.total = total;
		lists = new ArrayList<AmazonListView>();
	}
	
	public List<? extends AmazonListView> getLists() {
		return lists;
	}
	
	public void addList(AmazonListView list, boolean refreshTotal) {
		lists.add(list);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	}
	
	public void addLists(List<? extends AmazonListView> lists, boolean refreshTotal) {
		this.lists.addAll(lists);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	}

	public void addLists(Collection<? extends AmazonListView> lists, boolean refreshTotal) {
		this.lists.addAll(lists);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	}
	
	public int getTotal() {
		if (total >= 0)
			return total;
		else
			return lists.size();
	}
	
	public void setTotal(int total) {
		this.total = total;
	}
	
	public void setLists(List<? extends AmazonListView> lists, boolean refreshTotal) {
		this.lists.clear();
		this.lists.addAll(lists);
		if (refreshTotal) {
		    // clear the stored total
		    total = -1;
		}
	} 
	
	@Override
	public String toString() {
		return "{AmazonLists count=" + getTotal() + "}";
	}
}