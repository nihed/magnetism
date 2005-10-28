package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.PersonInfo;

public class EntityTag extends SimpleTagSupport {
	Object entity;
	
	static String entityHTML(Object o) {
		String link = null;
		String body;
		
		if (o instanceof PersonInfo) {
			PersonInfo info = (PersonInfo)o;
			link = "viewperson?personId=" + info.getPerson().getId();
			body = info.getHumanReadableName();
		} else if (o instanceof Group) {
			Group group = (Group)o;
			link = "viewgroup?groupId=" + group.getId();
			body = group.getName();
		} else {
			body = "???";
		}
		
		if (link != null)
			return "<a href='" + link + "\'>" + XmlBuilder.escape(body) + "</a>";  
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
