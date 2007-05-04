package com.dumbhippo.services;

import java.util.Collection;
import java.util.List;

public interface AmazonListsView {

	public List<? extends AmazonListView> getLists();

	public int getTotal();

	public void addList(AmazonListView list, boolean refreshTotal);
	
	public void addLists(List<? extends AmazonListView> lists, boolean refreshTotal);
	
	public void addLists(Collection<? extends AmazonListView> lists, boolean refreshTotal);
	
	public void setLists(List<? extends AmazonListView> lists, boolean refreshTotal);
}