package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

public class RelocateDestTag extends RelocationsTag {
	@Override
	public void doTag() throws IOException, JspException {
		JspWriter writer = getJspContext().getOut();
		doFragments(writer);
	}
}
