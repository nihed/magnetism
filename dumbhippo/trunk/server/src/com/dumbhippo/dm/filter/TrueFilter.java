package com.dumbhippo.dm.filter;

import java.util.List;

public class TrueFilter implements Filter {
	private static TrueFilter instance = new TrueFilter();
	
	public static TrueFilter getInstance() {
		return instance;
	}
	
	private TrueFilter() {
	}

	public void appendConditions(List<Condition> result) {
		return;
	}

	public Filter reduce(Condition condition, boolean value) {
		return this;
	}
	
	@Override
	public String toString() {
		return "true";
	}
}
