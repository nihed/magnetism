package com.dumbhippo.web.tags;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.PersonView;

/**
 * This is a very simple custom tag; it takes an argument that
 * is a list and an optional ID for list members to skip.
 * If there are list members that don't match the ID, it emits
 * the body of the tag, otherwise it emits  nothing.
 * 
 * @author otaylor
 */
public class SkipListTag extends SimpleTagSupport {
	List<? extends Object> value;
	String skipId;
	
	@Override
	public void doTag() throws IOException, JspException {
		if (value == null || value.size() == 0)
			return;
		
		if (skipId != null) {
			boolean foundNonSkipped = false;
			
			for (Object object : value) {
				if (object instanceof PersonView) {
					PersonView personView = (PersonView)object;
					if ((personView.getUser() != null ) && personView.getUser().getId().equals(skipId)) {
						continue;
					}	
				} else if (object instanceof GroupView) {
					GroupView groupView = (GroupView)object;
					if (groupView.getGroup().getId().equals(skipId)) {
						continue;
					}	
				} else if (object instanceof Group) {
					Group group = (Group)object;
					if (group.getId().equals(skipId)) {
						continue;
					}					
				}
				foundNonSkipped = true;
			}
			
			if (!foundNonSkipped)
				return;
		}
		
		if (getJspBody() != null)
			getJspBody().invoke(getJspContext().getOut());
	}
	
	public void setValue(List<? extends Object> value) {
		this.value = value;
	}

	public void setSkipId(String skipId) {
		this.skipId = skipId;
	}
}
