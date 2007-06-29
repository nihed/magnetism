package com.dumbhippo.dm.filter;

import java.util.List;

public class NotFilter implements Filter {
	Filter f;
	
	public NotFilter(Filter f) {
		this.f = f;
	}
	
	@Override
	public String toString() {
		return "!(" + f + ")";
	}

	public void appendConditions(List<Condition> result) {
		f.appendConditions(result);
	}

	public Filter reduce(Condition condition, boolean value) {
		Filter reducedF = f.reduce(condition, value);
		if (reducedF == f)
			return this;
		else if (reducedF == FalseFilter.getInstance())
			return TrueFilter.getInstance();
		else if (reducedF == TrueFilter.getInstance())
			return FalseFilter.getInstance();
		else
			return new NotFilter(reducedF);
	}
	
	@Override
	public int hashCode() {
		return (f.hashCode() + 29) * 37;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof NotFilter))
			return false;
		
		NotFilter other = (NotFilter)o;
		return f.equals(other.f);
	}

	public Filter asItemFilter() {
		return new NotFilter(f.asItemFilter());
	}
}
