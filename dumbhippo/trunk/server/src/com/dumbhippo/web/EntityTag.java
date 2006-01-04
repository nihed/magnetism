package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	static private final Log logger = GlobalSetup.getLog(EntityTag.class);
	
	private Object entity;
	private boolean showInviteLinks;
	private boolean photo;
	private String cssClass;
	private int bodyLengthLimit;
	
	static String entityHTML(JspContext context, Object o, String buildStamp, String skipId, boolean showInviteLinks, boolean photo,
			String cssClass, int bodyLengthLimit) {
		String link = null;
		String body;
		String photoUrl = null;
		String defaultCssClass = "dh-headshot";

		if (o instanceof PersonView) {
			PersonView view = (PersonView)o;
			User user = view.getUser();
			if (user != null) {
				String id = user.getId();
				if (skipId != null && skipId.equals(id))
					return null;
				link = "/person?who=" + id;
				photoUrl = view.getSmallPhotoUrl();
			}
			body = view.getName();
			defaultCssClass = "dh-person";
		} else if (o instanceof GroupView) {
			GroupView groupView = (GroupView)o;
			Group group = groupView.getGroup();
			if (skipId != null && skipId.equals(group.getId()))
				return null;
			PersonView inviter = groupView.getInviter();
			link = "/group?who=" + group.getId();
			if (inviter != null)
				body = group.getName() + " (invited by " + inviter.getName() + ")";
			else
				body = group.getName();
			photoUrl = groupView.getSmallPhotoUrl();
			defaultCssClass = "dh-group";
		} else if (o instanceof Group) {
			Group group = (Group)o;
			GroupView view = new GroupView(group, null, null);
			if (skipId != null && skipId.equals(group.getId()))
				return null;
			link = "/group?who=" + group.getId();
			body = group.getName();
			photoUrl = view.getSmallPhotoUrl();
			defaultCssClass = "dh-group";
		} else {
			if (o == null)
				logger.error("null object in EntityTag!");
			else
				logger.error("Weird object in EntityTag type " + o.getClass().getName() + ": " + o.toString());
			body = "???";
		}
		
		if (cssClass == null)
			cssClass = defaultCssClass;
		
		XmlBuilder xml = new XmlBuilder();

		String bodyOriginal = body;
		if (bodyLengthLimit != 0 && (body.length() > bodyLengthLimit)) {
			if (bodyLengthLimit > 3) {
				body = body.substring(0, bodyLengthLimit - 3);
				body += "...";
			} else {
				body = body.substring(0, bodyLengthLimit);
			}
		}
		
		if (photo && photoUrl != null) {
			if (link != null)
				xml.openElement("a", "href", link, "target", "_top", "class", cssClass, "title", bodyOriginal);

			String style = "width: " + Configuration.SHOT_SMALL_SIZE + "; height: " + Configuration.SHOT_SMALL_SIZE + ";"; 
			PngTag.pngHtml(context, xml, photoUrl, buildStamp, "dh-headshot", style);
			xml.appendEscaped(body);
		}
		else {
			/** For Listing Recipients in comma separated list  **/
			xml.appendTextNode("a", body, "href", link, "target", "_top");
		}
		
		if (showInviteLinks && o instanceof PersonView && !((PersonView)o).isInvited()) {
			PersonView view = (PersonView)o;
			xml.append(" (");
			String inviteUrl = "/invite?fullName=" + StringUtils.urlEncode(view.getName()) + "&email=" + StringUtils.urlEncode(view.getEmail().getEmail()); 
			xml.appendTextNode("a", "invite", "href", inviteUrl);
			xml.append(")");
		}

		if (photo && photoUrl != null && link != null)
			xml.closeElement();

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
		writer.print(entityHTML(getJspContext(), entity, buildStamp, null, showInviteLinks, photo, cssClass, bodyLengthLimit));
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

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	public void setBodyLengthLimit(int bodyLengthLimit) {
		this.bodyLengthLimit = bodyLengthLimit;
	}
}
