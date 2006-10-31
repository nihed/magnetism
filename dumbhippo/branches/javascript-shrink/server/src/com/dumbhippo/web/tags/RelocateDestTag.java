package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * Print the content of an earlier RelocateTag with the same 
 * "where" property.
 * 
 * @author Havoc Pennington
 *
 */
public class RelocateDestTag extends RelocationsTag {
	@Override
	public void doTag() throws IOException, JspException {
		JspWriter writer = getJspContext().getOut();
		doFragments(writer);
	}
}
