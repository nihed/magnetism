package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PostView;

/**
 * Tag to display presence, including whether a user is online or offline,
 * whether there is an active chat for a post or group.
 * 
 * @author dff
 */

public class PresenceTag extends SimpleTagSupport {
	Object entity;
	
	static String presenceHTML(Object o, String skipId) {	
		String returnString = "";
		
		if (o instanceof PersonView) {
			PersonView view = (PersonView)o;
			AimResource primaryAim = view.getAim();
			if (primaryAim != null) {
				// Is this right? isOnline is the Mugshot status, not
				// the aim status, but if you've entered your aim address into the
				// account page, maybe they are close enough?
				if (view.isOnline()) {
					returnString = "<a href=\"aim:GoIm?ScreenName=" + primaryAim.getScreenName() + "\" alt=\"Send an message to " + primaryAim.getScreenName() + " via AIM\"><img src=\"/images/online.gif\" height=16 width=16 border=0 valign=center></a>";		
				}
			}
		} else if (o instanceof GroupView) {
			// TODO: Finish this, including accompanying GroupView work
		} else if (o instanceof PostView) {
			PostView postView = (PostView)o;
			if (postView.isChatRoomActive()) {
				returnString = "<a href='javascript:dh.actions.requestJoinRoom(\"" + postView.getPost().getId() + "\")' alt=\"" + postView.getChatRoomMembers() + "\"><img src=\"/images/online.gif\" height=16 width=16 border=0 valign=center></a>";
			}
		}
		
		return returnString;
	}
	
	@Override
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		writer.print(presenceHTML(entity, null));
	}
	
	public void setValue(Object value) {
		entity = value;
	}
}
