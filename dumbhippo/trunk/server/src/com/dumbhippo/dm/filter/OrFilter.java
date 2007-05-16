package com.dumbhippo.dm.filter;

public class OrFilter implements Filter {
	Filter a;
	Filter b;
	
	public OrFilter(Filter a, Filter b) {
		this.a = a;
		this.b = b;
	}
	
	@Override
	public String toString() {
		return "(" + a + ")" + "||" + "(" + b +")";
	}
}
