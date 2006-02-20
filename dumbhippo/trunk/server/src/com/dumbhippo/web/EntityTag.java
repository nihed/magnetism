package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

public class EntityTag extends SimpleTagSupport {
	static private final Logger logger = GlobalSetup.getLogger(EntityTag.class);
	
	private Object entity;
	private boolean showInviteLinks;
	private boolean photo;
	private String cssClass;
	private int bodyLengthLimit;
	private boolean music;	
	private boolean twoLineBody;
	
	public EntityTag() {
		bodyLengthLimit = -1;
		twoLineBody = false;
	}
	
	static String entityHTML(JspContext context, Object o, String buildStamp, String skipId,
			boolean showInviteLinks, boolean photo, boolean music,
			String cssClass, int bodyLengthLimit, boolean twoLineBody) {
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
				if (music)
					link = "/music?who=" + id;
				else
					link = "/person?who=" + id;
				photoUrl = view.getSmallPhotoUrl();
				defaultCssClass = "dh-person";
			} else {
				defaultCssClass = "dh-listed-person";
			}
			body = view.getName();
		} else if (o instanceof GroupView) {
			GroupView groupView = (GroupView)o;
			Group group = groupView.getGroup();
			if (skipId != null && skipId.equals(group.getId()))
				return null;
			PersonView inviter = groupView.getInviter();
			if (music)
				link = "/musicgroup?who=" + group.getId();
			else
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
			if (music)
				link = "/musicgroup?who=" + group.getId();
			else
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

		// truncateString would return the original String if bodyLengthLimit is negative
		String bodyOriginal = body;
		String longerBody = 
			StringUtils.truncateString(bodyOriginal, bodyLengthLimit*3);
		
		int finalIndexOfSpace = -1;
		
		if (twoLineBody) {
		    // this is a String that can be displayed over the two lines, so if it starts with a word or
		    // a number of words that are shorter than bodyLengthLimit, leave them intact, and truncate 
		    // after them
		    int indexOfSpace = bodyOriginal.indexOf(" "); 

		    // because indexOfSpace is 0-based, when indexOfSpace is equal to bodyLengthLimit, 
		    // it means that the space is the bodyLengthLimit+1st character, so we still want
		    // to leave the String up to that point for the first line
		    while ((indexOfSpace > 0) && (indexOfSpace <= bodyLengthLimit)) {
			    finalIndexOfSpace = indexOfSpace;
			    indexOfSpace = bodyOriginal.indexOf(" ", finalIndexOfSpace+1);			
		    }
		}
		
		// if finalIndexOfSpace is -1, than -1+1 will be 0, and we do not need a special case
		// to check that
		body = StringUtils.truncateString(bodyOriginal, finalIndexOfSpace+1+bodyLengthLimit);
		
		boolean openElement = false;
		if (photo && photoUrl != null) {
			if (link != null) {
				xml.openElement("a", "href", link, "target", "_top", "class", cssClass, "title", bodyOriginal);
				openElement = true;
			}
			
			String style = "width: " + Configuration.SHOT_SMALL_SIZE + "; height: " + Configuration.SHOT_SMALL_SIZE + ";"; 
			PngTag.pngHtml(context, xml, photoUrl, buildStamp, "dh-headshot", style);
			xml.append("<br/>");
			xml.appendEscaped(body);
		}
		else {
			// for listing recipients in comma separated list or
			// for listing contacts with no accounts
			if (link != null) { 
			    xml.appendTextNode("a", body, "href", link, "target", "_top");
			} else {
				xml.openElement("div", "class", cssClass, "title", bodyOriginal);
				openElement=true;
				xml.appendEscaped(longerBody);
			}
		}
		
		if (showInviteLinks && o instanceof PersonView && !((PersonView)o).isInvited()) {
			PersonView view = (PersonView)o;
			xml.append(" (");
			String inviteUrl = "/invite?fullName=" + StringUtils.urlEncode(view.getName()) + "&email=" + StringUtils.urlEncode(view.getEmail().getEmail()); 
			xml.appendTextNode("a", "invite", "href", inviteUrl);
			xml.append(")");
		}

		if (openElement) {
			xml.closeElement();
			openElement = false;
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
		writer.print(entityHTML(getJspContext(), entity, buildStamp, null, showInviteLinks, 
				                photo, music, cssClass, bodyLengthLimit, twoLineBody));
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

	public void setMusic(boolean music) {
		this.music = music;
	}
	
	public void setTwoLineBody(boolean twoLineBody) {
		this.twoLineBody = twoLineBody;
	}
}
