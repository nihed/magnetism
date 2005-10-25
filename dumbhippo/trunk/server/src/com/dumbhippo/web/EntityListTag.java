package com.dumbhippo.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class EntityListTag extends SimpleTagSupport {
	List<Object> entities;
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		
		boolean first = true;
		for (Object o : entities) {
			if (!first)
				writer.print(", ");
			
			writer.print(EntityTag.entityHTML(o));
			
			first = false;
		}
	}
	
	public void setValue(List<Object> value) {
		entities = value;
	}
}
