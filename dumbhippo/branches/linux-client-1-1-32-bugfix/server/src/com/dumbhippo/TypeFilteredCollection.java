/**
 * 
 */
package com.dumbhippo;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TypeFilteredCollection<From,To> extends AbstractCollection<To> {
	private Collection<From> delegate;
	private Class<To> toClass;
	private int size;
	
	public TypeFilteredCollection(Collection<From> delegate, Class<To> toClass) {
		this.delegate = delegate;
		this.toClass = toClass;
		this.size = -1;
	}

	private class TypeFilteredIterator implements Iterator<To> {

		Iterator<From> fromIterator;
		To next;
		
		TypeFilteredIterator() {
			this.fromIterator = delegate.iterator();
			next = null;
		}
		
		private void findNext() {
			if (next != null)
				return;
			
			while (fromIterator.hasNext()) {
				From possible = fromIterator.next();
				if (possible == null)
					throw new NullPointerException("null not allowed in this collection");
				try {
					next = toClass.cast(possible);
					return;
				} catch (ClassCastException e) {
				}
			}
			return;
		}
		
		public boolean hasNext() {
			findNext();
			return next != null;
		}

		public To next() {
			findNext();
			if (next == null)
				throw new NoSuchElementException("no more elements");
			To ret = next;
			next = null;
			return ret;
		}

		public void remove() {
			throw new UnsupportedOperationException("read-only iterator");
		}			
	}
	
	@Override
	public Iterator<To> iterator() {
		return new TypeFilteredIterator();
	}

	@Override
	public int size() {
		if (size < 0) {
			size = 0;
			Iterator<To> i = iterator();
			while (i.hasNext()) {
				i.next();
				++size;
			}
		}
		return size;
	}
}
