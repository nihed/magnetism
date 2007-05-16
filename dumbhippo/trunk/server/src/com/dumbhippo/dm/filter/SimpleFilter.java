package com.dumbhippo.dm.filter;

public class SimpleFilter implements Filter {
	private String predicateName;
	private FilterTermType type;
	private String propertyName;
	
	public SimpleFilter(String predicateName, FilterTermType type, String propertyName) {
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
}
