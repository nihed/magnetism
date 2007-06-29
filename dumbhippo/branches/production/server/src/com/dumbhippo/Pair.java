package com.dumbhippo;

import java.io.Serializable;

/**
 * Lame way to have two return values from a method.
 * Needs to be Serializable since used as a key for
 * caching stuff we get from web services.
 * 
 * @author hp
 *
 * @param <A> the type of the first half of the pair
 * @param <B> the type of the second half of the pair
 */
public class Pair<A,B> implements Serializable {
	
	private static final long serialVersionUID = 0L;
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
	
	public void setFirst(A first) {
		this.first = first;		
	}
	
	public void setSecond(B second) {
		this.second = second;
	}
}
