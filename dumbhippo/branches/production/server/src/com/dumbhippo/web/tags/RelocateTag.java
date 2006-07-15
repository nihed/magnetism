package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;

/**
 * This tag moves some markup from one place to another,
 * later place. A RelocateDest tag with the same "where" marks where the
 * stuff goes.
 * 
 * @author Havoc Pennington
 *
 */
public class RelocateTag extends RelocationsTag { 
	
	@Override
	public void doTag() throws IOException, JspException {
		addFragment(getJspBody());
	}
}
