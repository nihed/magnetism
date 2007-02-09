package com.dumbhippo;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

public class TypeFilteredCollectionTest extends TestCase {

	private static class Supertype {
		private int i;
		
		Supertype(int i) {
			this.i = i;
		}
		
		@Override
		public int hashCode() {
			return i;
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Supertype)) {
				return false;
			}
			return ((Supertype)other).i == i;
		}
		
		@Override
		public String toString() {
			return "'" + Integer.toString(i) + " " + getClass().getName() + "'";
		}
	}
	private static class SubtypeA extends Supertype {
		SubtypeA(int i) {
			super(i);
		}
	}
	private static class SubtypeB extends Supertype {
		SubtypeB(int i) {
			super(i);
		}		
	}
	
	public void testTypeFilteredCollection() {
		int aCount = 0;
		int bCount = 0;
		final int ORIGINAL_SIZE = 23;
		
		Collection<Supertype> original = new HashSet<Supertype>();
		
		for (int i = 0; i < ORIGINAL_SIZE; ++i) {
			if (i < 10) {
				original.add(new SubtypeA(i));
				++aCount;
			} else {
				original.add(new SubtypeB(i));
				++bCount;
			}
		}
		
		assertEquals(original.size(), ORIGINAL_SIZE);
		
		TypeFilteredCollection<Supertype, SubtypeA> filteredA = new TypeFilteredCollection<Supertype, SubtypeA>(original, SubtypeA.class);
		TypeFilteredCollection<Supertype, SubtypeB> filteredB = new TypeFilteredCollection<Supertype, SubtypeB>(original, SubtypeB.class);
		
		int counted = 0;
		for (SubtypeA t : filteredA) {
			t.hashCode(); // suppress unused, "suppress warnings" annotation crashes javac
			//System.out.println("A has " + t);
			++counted;
		}
		assertEquals(counted, aCount);
		assertEquals(filteredA.size(), aCount);
		
		counted = 0;
		for (SubtypeB t : filteredB) {
			t.hashCode(); // suppress unused, "suppress warnings" annotation crashes javac
			//System.out.println("B has " + t);
			++counted;
		}
		assertEquals(counted, bCount);
		assertEquals(filteredB.size(), bCount);
		
		Collection<Supertype> reconstructed = new HashSet<Supertype>();
		reconstructed.addAll(filteredA);
		reconstructed.addAll(filteredB);
		
		assertEquals(reconstructed.size(), original.size());
		
		assertTrue(original.equals(reconstructed));
	}
}
