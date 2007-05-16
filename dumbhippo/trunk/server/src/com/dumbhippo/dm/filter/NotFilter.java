package com.dumbhippo.dm.filter;

public class NotFilter implements Filter {
	Filter f;
	
	public NotFilter(Filter f) {
		this.f = f;
	}
	
	@Override
	public String toString() {
		return "!(" + f + ")";
	}
}
