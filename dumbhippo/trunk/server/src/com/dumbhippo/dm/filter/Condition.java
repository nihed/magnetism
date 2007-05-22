package com.dumbhippo.dm.filter;

import java.util.List;

/**
 * A condition is a filter with no child nodes... it has a 'predicate' which is just
 * a method on the DMViewpoint subclass, a type (key,item,all,any), and possibly
 * a property to fetch from the key/item before applying the predicate.
 * 
 * @author otaylor
 */
public class Condition implements Filter {
	private String predicateName;
	private ConditionType type;
	private String propertyName;
	
	public Condition(String predicateName, ConditionType type, String propertyName) {
		this.predicateName = predicateName;
		this.type = type;
		this.propertyName = propertyName;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append("viewer.");
		b.append(predicateName);
		b.append("(");
		b.append(type.getLowerName());
		if (propertyName != null) {
			b.append(".");
			b.append(propertyName);
		}
		b.append(")");
		
		return b.toString();
	}

	public String getPredicateName() {
		return predicateName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public ConditionType getType() {
		return type;
	}

	public void appendConditions(List<Condition> result) {
		result.add(this);
	}
	
	public Filter reduce(Condition condition, boolean value) {
		if (condition == this)
			return value ? TrueFilter.getInstance() : FalseFilter.getInstance();
		else
			return this;
	}
}
