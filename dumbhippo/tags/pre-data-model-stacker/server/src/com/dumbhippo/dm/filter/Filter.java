package com.dumbhippo.dm.filter;

import java.util.List;

public interface Filter {
	public void appendConditions(List<Condition> result);
	public Filter reduce(Condition condition, boolean value);
	public Filter asItemFilter();
}
