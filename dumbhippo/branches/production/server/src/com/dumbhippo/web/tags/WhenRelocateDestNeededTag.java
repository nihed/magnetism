package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * Output content only if a RelocateDestTag with the same "where" 
 * property would have something to output.
 * 
 * @author Havoc Pennington
 *
 */
public class WhenRelocateDestNeededTag extends RelocationsTag {
	@Override
	public void doTag() throws IOException, JspException {
		if (haveFragments()) {
			JspWriter writer = getJspContext().getOut();
			getJspBody().invoke(writer);
		} else {
			// no-op tag - don't print the contents
		}
	}
}
