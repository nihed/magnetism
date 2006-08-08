package com.dumbhippo.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class provides information about what data columns are contained
 * in a recording of statistics data. The exposed interface is similar
 * to a List<ColumnDescription> with some extra methods.
 * @author otaylor
 */
public class ColumnMap implements Iterable<ColumnDescription> {
	List<ColumnDescription> columns = new ArrayList<ColumnDescription>();
	Map<String, Integer> map = new HashMap<String, Integer>();
	
	public ColumnMap() {
	}

	public ColumnMap(List<ColumnDescription> columns) {
		for (ColumnDescription column : columns )
			add(column);
	}
	
	public void add(ColumnDescription column) {
		columns.add(column);
		map.put(column.getId(), columns.size() - 1);
	}
	
	public int size() {
		return columns.size();
	}
	
	public List<ColumnDescription> getColumns() {
		return columns;
	}
	
	public ColumnDescription get(int index) {
		return columns.get(index);
	}
	
	public Iterator<ColumnDescription> iterator() {
		return columns.iterator();
	}
	
	public ColumnDescription get(String id) {
		return columns.get(getIndex(id));
		
	}
	
	/**
	 * Finds the index within the map of the column with the given string ID 
	 * @param id string identifier of the column (See ColumnDescription.getId)
	 * @return the column index
	 * @throws NoSuchElementException if no column with that ID is present 
	 */
	public int getIndex(String id) {
		Integer index = map.get(id);
		if (index == null)
			throw new NoSuchElementException();
		else
			return index;
	}
}
