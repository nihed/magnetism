package com.dumbhippo.dm.filter;

import java.util.List;

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
	
	public void appendConditions(List<Condition> result) {
		a.appendConditions(result);
		b.appendConditions(result);
	}

	public Filter reduce(Condition condition, boolean value) {
		Filter reducedA = a.reduce(condition, value);
		Filter reducedB = b.reduce(condition, value);
		
		if (reducedA == a && reducedB == b)
			return this;
		else if (reducedA == FalseFilter.getInstance() || reducedB == FalseFilter.getInstance())
			return FalseFilter.getInstance();
		else if (reducedA == TrueFilter.getInstance())
			return reducedB;
		else if (reducedB == TrueFilter.getInstance())
			return reducedA;
		else
			return new AndFilter(reducedA, reducedB);
	}
	
	@Override
	public int hashCode() {
		return a.hashCode() * 17 + b.hashCode() * 23;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AndFilter))
			return false;
		
		AndFilter other = (AndFilter)o;
		return a.equals(other.a) && b.equals(other.b);
	}
}
