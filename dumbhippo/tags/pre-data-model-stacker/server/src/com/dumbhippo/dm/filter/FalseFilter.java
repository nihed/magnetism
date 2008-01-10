package com.dumbhippo.dm.filter;

import java.util.List;

public class FalseFilter implements Filter {
	private static FalseFilter instance = new FalseFilter();
	
	public static FalseFilter getInstance() {
		return instance;
	}
	
	private FalseFilter() {
	}

	public void appendConditions(List<Condition> result) {
		return;
	}

	public Filter reduce(Condition condition, boolean value) {
		return this;
	}
	
	@Override
	public String toString() {
		return "false";
	}

	public Filter asItemFilter() {
		return this;
	}
}
