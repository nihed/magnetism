package com.dumbhippo;

/**
 * Lame way to have two return values from a method.
 * 
 * @author hp
 *
 * @param <A> the type of the first half of the pair
 * @param <B> the type of the second half of the pair
 */
public class Pair<A,B> {
	A first;
	B second;
	
	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}
	
	public A getFirst() {
		return first;
	}
	
	public B getSecond() {
		return second;
	}
}
