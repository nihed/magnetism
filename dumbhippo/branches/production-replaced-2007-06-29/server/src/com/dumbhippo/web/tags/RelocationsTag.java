package com.dumbhippo.web.tags;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/** 
 * This is a kind of bogus base class shared by the tags that 
 * manipulate "relocations" within the jsp page. It would be more 
 * OO-correct to use composition instead of inheritance for this.
 * 
 * @author Havoc Pennington
 *
 */
public abstract class RelocationsTag extends SimpleTagSupport {
	
	@SuppressWarnings("unused")
	protected static final Logger logger = GlobalSetup.getLogger(RelocationsTag.class);
	
	// This seems wrong to me, PAGE_SCOPE makes more sense, but doesn't appear to work
	private static final int SCOPE = PageContext.REQUEST_SCOPE;
	
	private String where;
	
	public void setWhere(String where) {
		this.where = where;
	}

	@SuppressWarnings("unchecked")
	private List<String> getFragmentsList() {
		JspContext jspContext = getJspContext();
		
		String attrName = "relocation_" + where;
		List<String> list = null;
		
		Object obj = jspContext.getAttribute(attrName, SCOPE);
		if (obj == null) {
			list = new ArrayList<String>();
			jspContext.setAttribute(attrName, list, SCOPE);
		} else {
			list = (List)obj;
		}
		
		return list;
	}
	
	protected boolean haveFragments() {
		return getFragmentsList().size() > 0;
	}
	
	protected void addFragment(JspFragment fragment) throws JspException, IOException {
		Writer writer = new StringWriter();
		fragment.invoke(writer);
		String s = writer.toString();
		getFragmentsList().add(s);
	}
	
	protected void doFragments(Writer writer) throws JspException, IOException {
		List<String> fragments = getFragmentsList();
		for (String f : fragments) {
			writer.write(f);
		}
	}
}
