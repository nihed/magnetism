package com.dumbhippo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Elements a thread-safe list of listeners. Listeners can be added
 * or removed from any thread and calling iterator() provides a 
 * snapshot of the current listener list. Note that this doesn't
 * actually provide any mechanism for invoking listeners; that
 * has to be done by the caller.
 * 
 * @author otaylor
 *
 * @param <T> the type of listener object
 */
public class ListenerList<T> implements Iterable<T> {
	private List<T> listeners = new ArrayList<T>();
	
	/**
	 * Adds a listener to the list of listeners
	 * @param listener
	 */
	public synchronized void addListener(T listener) {
		listeners.add(listener);
	}
	
	/**
	 * Removes a listener previously added with addListener. Note that 
	 * if an invocation of this listener list is currently in progress,
	 * a listener may still be invoked after removeListener is called.
	 * @param listener
	 */
	public synchronized void removeListener(T listener) {
		// We need pointer equality not equals(), so we can't use Array.remove()
		for (Iterator<T> i = listeners.iterator(); i.hasNext();) {
			if (i.next() == listener) {
				i.remove();
				return;
			}
		}
	}
	
	/**
	 * @return true if there are no listeners in the list
	 */
	public synchronized boolean isEmpty() {
		return listeners.isEmpty();
	}
	

	/**
	 * Provides a snapshot of the current state of the listener list that
	 * the caller can use to invoke all the listeners in the list.
	 * 
	 * @return an Iterator that walks over a snapshot of the listener list
	 */
	public synchronized Iterator<T> iterator() {
		return new ListenerIterator<T>(listeners.toArray());
	}
	
	private static class ListenerIterator<U> implements Iterator<U> {
		private Object[] elements;
		private int position;
		
		public ListenerIterator(Object[] elements) {
			this.elements = elements;
			this.position = 0;
		}

		public boolean hasNext() {
			return position < elements.length;
		}

		public U next() {
			if (position == elements.length)
				throw new NoSuchElementException();
			
			@SuppressWarnings("unchecked")
			U u = (U)elements[position++];
			
			return u;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
