package com.dumbhippo.dm.filter;

import java.util.List;

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

	public void appendConditions(List<Condition> result) {
		a.appendConditions(result);
		b.appendConditions(result);
	}
	

	public Filter reduce(Condition condition, boolean value) {
		Filter reducedA = a.reduce(condition, value);
		Filter reducedB = b.reduce(condition, value);
		
		if (reducedA == a && reducedB == b)
			return this;
		else if (reducedA == TrueFilter.getInstance() || reducedB == TrueFilter.getInstance())
			return TrueFilter.getInstance();
		else if (reducedA == FalseFilter.getInstance())
			return reducedB;
		else if (reducedB == FalseFilter.getInstance())
			return reducedA;
		else
			return new OrFilter(reducedA, reducedB);
	}
	
	@Override
	public int hashCode() {
		return a.hashCode() * 13 + b.hashCode() * 19;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof OrFilter))
			return false;
		
		OrFilter other = (OrFilter)o;
		return a.equals(other.a) && b.equals(other.b);
	}
}
