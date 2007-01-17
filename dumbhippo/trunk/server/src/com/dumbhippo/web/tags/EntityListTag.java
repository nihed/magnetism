package com.dumbhippo.web.tags;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PostView;


public class EntityListTag extends SimpleTagSupport {
	private List<Object> entities;
	private String skipRecipientId;
	private String cssClass;
	private boolean showInviteLinks;
	private boolean photos;
	private int bodyLengthLimit;
	private int longBodyLengthLimit;
	private String prefixValue;
	private String separator;
	private boolean music;
	private boolean twoLineBody;
	
	public EntityListTag() {
		bodyLengthLimit = -1;
		longBodyLengthLimit = -1;
		twoLineBody = false;
		prefixValue = "";
	}

	static private String presenceHTML(Object o, String skipId) {	
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
		
		if (entities == null)
			return;
		
		String buildStamp;
		try {
			buildStamp = (String) getJspContext().getVariableResolver().resolveVariable("buildStamp");
		} catch (ELException e) {
			throw new RuntimeException(e);
		}
		
		Iterator it = entities.iterator();
		
		boolean first = true;
		
		if (prefixValue != null && !prefixValue.equals("")) {
			writer.print(prefixValue);
			first = false;
		}
		
		while (it.hasNext()) {
			Object o = it.next();
			
			String html = 
				EntityTag.entityHTML(getJspContext(), o, buildStamp, 
						             skipRecipientId, showInviteLinks, 
					                 photos, music, cssClass, 
					                 bodyLengthLimit, longBodyLengthLimit, 
					                 twoLineBody);
            String presenceHtml = presenceHTML(o, skipRecipientId);

            if (html == null)
                continue;
            
			if (separator != null && !first)
				writer.print(separator);
            first = false;
            
			if (presenceHtml != null) {
				html = html + presenceHtml;
			}
				
			writer.print(html);
		}
	}
	
	public void setValue(List<Object> value) {
		entities = value;
	}
	
	public void setSkipRecipientId(String skipRecipientId) {
		this.skipRecipientId = skipRecipientId;
	}
	
	public void setShowInviteLinks(boolean showInviteLinks) {
		this.showInviteLinks = showInviteLinks;
	}
	
	public void setPhotos(boolean photos) {
		this.photos = photos;
	}
	
	public void entityCssClass(String klass) {
		this.cssClass = klass;
	}

	public void setBodyLengthLimit(int bodyLengthLimit) {
		this.bodyLengthLimit = bodyLengthLimit;
	}

	public void setLongBodyLengthLimit(int longBodyLengthLimit) {
		this.longBodyLengthLimit = longBodyLengthLimit;
	}
	
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	public void setPrefixValue(String prefixValue) {
		this.prefixValue = prefixValue;
	}

	public void setMusic(boolean music) {
		this.music = music;
	}

	public void setTwoLineBody(boolean twoLineBody) {
		this.twoLineBody = twoLineBody;
	}
}
