package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	Object entity;
	
	static String entityHTML(Object o, String skipId) {
		String link = null;
		String body;
		
		if (o instanceof PersonView) {
			PersonView view = (PersonView)o;
			String id = view.getViewPersonPageId();
			if (id != null) {
				if (skipId != null && skipId.equals(id))
					return null;
				link = "viewperson?personId=" + id;
			}
			body = view.getName();
		} else if (o instanceof GroupView) {
			GroupView groupView = (GroupView)o;
			Group group = groupView.getGroup();
			if (skipId != null && skipId.equals(group.getId()))
				return null;
			PersonView inviter = groupView.getInviter();
			link = "viewgroup?groupId=" + group.getId();
			if (inviter != null)
				body = group.getName() + " (invited by " + inviter.getName() + ")";
			else
				body = group.getName();
		} else if (o instanceof Group) {
			Group group = (Group)o;
			if (skipId != null && skipId.equals(group.getId()))
				return null;
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
		writer.print(entityHTML(entity, null));
	}
	
	public void setValue(Object value) {
		entity = value;
	}
}
