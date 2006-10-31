package com.dumbhippo.web.tags;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.SimpleTagSupport;


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
            String presenceHtml = PresenceTag.presenceHTML(o, skipRecipientId);

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
