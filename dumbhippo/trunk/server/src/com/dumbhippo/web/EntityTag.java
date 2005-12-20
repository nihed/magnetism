package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	private Object entity;
	private boolean showInviteLinks;
	private boolean photo;
	
	static String entityHTML(JspContext context, Object o, String buildStamp, String skipId, boolean showInviteLinks, boolean photo) {
		String link = null;
		String body;
		String photoUrl = null;
		
		if (o instanceof PersonView) {
			PersonView view = (PersonView)o;
			User user = view.getUser();
			if (user != null) {
				String id = user.getId();
				if (skipId != null && skipId.equals(id))
					return null;
				link = "/viewperson?personId=" + id;
				photoUrl = AbstractPhotoServlet.getPersonSmallPhotoUrl(id, user.getVersion());
			}
			body = view.getName();
		} else if (o instanceof GroupView) {
			GroupView groupView = (GroupView)o;
			Group group = groupView.getGroup();
			if (skipId != null && skipId.equals(group.getId()))
				return null;
			PersonView inviter = groupView.getInviter();
			link = "/viewgroup?groupId=" + group.getId();
			if (inviter != null)
				body = group.getName() + " (invited by " + inviter.getName() + ")";
			else
				body = group.getName();
			photoUrl = AbstractPhotoServlet.getGroupSmallPhotoUrl(group.getId(), group.getVersion());
		} else if (o instanceof Group) {
			Group group = (Group)o;
			if (skipId != null && skipId.equals(group.getId()))
				return null;
			link = "/viewgroup?groupId=" + group.getId();
			body = group.getName();
			photoUrl = AbstractPhotoServlet.getGroupSmallPhotoUrl(group.getId(), group.getVersion());
		} else {
			body = "???";
		}
		
		XmlBuilder xml = new XmlBuilder();
		
		if (photo && photoUrl != null) {
			if (link != null)
				xml.openElement("a", "href", link);
			String style = "width: " + Configuration.SHOT_SMALL_SIZE + "; height: " + Configuration.SHOT_SMALL_SIZE + ";"; 
			PngTag.pngHtml(context, xml, photoUrl, buildStamp, "dh-headshot", style);
			if (link != null)
				xml.closeElement();
		}
		
		if (link != null)
			xml.appendTextNode("a", body, "href", link, "target", "_top");
		else
			xml.appendEscaped(body);
		
		if (showInviteLinks && o instanceof PersonView && !((PersonView)o).isInvited()) {
			PersonView view = (PersonView)o;
			xml.append(" (");
			String inviteUrl = "/invite?fullName=" + StringUtils.urlEncode(view.getName()) + "&email=" + StringUtils.urlEncode(view.getEmail().getEmail()); 
			xml.appendTextNode("a", "invite", "href", inviteUrl);
			xml.append(")");
		}
		return xml.toString();
	}
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		String buildStamp;
		try {
			buildStamp = (String) getJspContext().getVariableResolver().resolveVariable("buildStamp");
		} catch (ELException e) {
			throw new RuntimeException(e);
		}
		writer.print(entityHTML(getJspContext(), entity, buildStamp, null, showInviteLinks, photo));
	}
	
	public void setValue(Object value) {
		entity = value;
	}
	
	public void setShowInviteLinks(boolean showInviteLinks) {
		this.showInviteLinks = showInviteLinks;
	}
	
	public void setPhoto(boolean photo) {
		this.photo = photo;
	}
}
