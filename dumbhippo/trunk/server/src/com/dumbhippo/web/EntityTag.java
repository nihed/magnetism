package com.dumbhippo.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	private Object entity;
	private boolean showInviteLinks;
	
	private static String urlEncode(String in) {
		try {
			return URLEncoder.encode(in, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// it's ridiculous that this is a checked exception
			throw new RuntimeException(e);
		}
	}
	
	static String entityHTML(Object o, String skipId, boolean showInviteLinks) {
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
		
		XmlBuilder xml = new XmlBuilder();
		if (link != null)
			xml.appendTextNode("a", body, "href", link, "target", "_top");
		else
			xml.appendEscaped(body);
		if (showInviteLinks && o instanceof PersonView && !((PersonView)o).isInvited()) {
			PersonView view = (PersonView)o;
			xml.append(" (");
			String inviteUrl = "/invite?fullName=" + urlEncode(view.getName()) + "&email=" + urlEncode(view.getEmail().getEmail()); 
			xml.appendTextNode("a", "invite", "href", inviteUrl);
			xml.append(")");
		}
		return xml.toString();
	}
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		writer.print(entityHTML(entity, null, showInviteLinks));
	}
	
	public void setValue(Object value) {
		entity = value;
	}
	
	public void setShowInviteLinks(boolean showInviteLinks) {
		this.showInviteLinks = showInviteLinks;
	}
}
