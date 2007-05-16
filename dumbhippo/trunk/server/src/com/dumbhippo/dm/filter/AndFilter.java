package com.dumbhippo.dm.filter;

public class AndFilter implements Filter {
	Filter a;
	Filter b;
	
	public AndFilter(Filter a, Filter b) {
		this.a = a;
		this.b = b;
	}
	
	@Override
	public String toString() {
		return "(" + a + ")" + "&&" + "(" + b +")";
	}
}
