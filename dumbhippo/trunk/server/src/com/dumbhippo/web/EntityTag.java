package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	Object entity;
	
	static String entityHTML(Object o) {
		String link = null;
		String body;
		
		if (o instanceof PersonView) {
			PersonView view = (PersonView)o;
			link = "viewperson?personId=" + view.getPerson().getId();
			body = view.getHumanReadableName();
		} else if (o instanceof Group) {
			Group group = (Group)o;
			link = "viewgroup?groupId=" + group.getId();
			body = group.getName();
		} else {
			body = "???";
		}
		
		if (link != null)
			return "<a href='" + link + "\' target=_top>" + XmlBuilder.escape(body) + "</a>";  
		else  
			return body;
	}
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		writer.print(entityHTML(entity));
	}
	
	public void setValue(Object value) {
		entity = value;
	}
}
