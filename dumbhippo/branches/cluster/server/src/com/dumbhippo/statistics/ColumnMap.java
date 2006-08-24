package com.dumbhippo.statistics;

import java.util.ArrayList;
import java.util.Collection;
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
public class ColumnMap implements Collection<ColumnDescription> {
	List<ColumnDescription> columns = new ArrayList<ColumnDescription>();
	Map<String, Integer> map = new HashMap<String, Integer>();
	
	public ColumnMap() {
	}

	public ColumnMap(List<ColumnDescription> columns) {
		for (ColumnDescription column : columns )
			add(column);
	}
	
	public boolean add(ColumnDescription column) {
		columns.add(column);
		map.put(column.getId(), columns.size() - 1);
		
		return true;
	}
	
	public int size() {
		return columns.size();
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
	
	// The rest of this gunk is purely because <c:foreach/> doesn't support
	// Iterarable, so we have to make ColumnMap a collection.

	public boolean isEmpty() {
		return columns.isEmpty();
	}

	public boolean contains(Object arg0) {
		return columns.contains(arg0);
	}

	public Object[] toArray() {
		return columns.toArray();
	}

	public <T> T[] toArray(T[] arg0) {
		return columns.toArray(arg0);
	}

	public boolean containsAll(Collection<?> arg0) {
		return columns.containsAll(arg0);
	}

	public boolean addAll(Collection<? extends ColumnDescription> arg0) {
		for (ColumnDescription cd : arg0)
			add(cd);

		return true;
	}

	public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}
}
