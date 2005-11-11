package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	Object entity;
	
	static String entityHTML(Object o) {
		String link = null;
		String body;
		
		if (o instanceof PersonView) {
			PersonView view = (PersonView)o;
			Person person = view.getPerson();
			if (person != null)
				link = "viewperson?personId=" + person.getId();
			body = view.getHumanReadableName();
		} else if (o instanceof GroupView) {
			GroupView groupView = (GroupView)o;
			Group group = groupView.getGroup();
			PersonView inviter = groupView.getInviter();
			link = "viewgroup?groupId=" + group.getId();
			if (inviter != null)
				body = group.getName() + " (invited by " + inviter.getHumanReadableName() + ")";
			else
				body = group.getName();
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
