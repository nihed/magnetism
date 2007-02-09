package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.web.Scope;

public class DefaultTag extends SimpleTagSupport {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DefaultTag.class);
	
	String var;
	Scope scope = Scope.PAGE;
	Object value;

	private Object getFromScope(Scope s, String key) {
		PageContext context = (PageContext)getJspContext();
		
		switch (s) {
		case PAGE:
			return context.getAttribute(key);
		case REQUEST:
			return context.getRequest().getAttribute(key);
		case SESSION:
			return context.getSession().getAttribute(key);
		case APPLICATION:
			return context.getServletContext().getAttribute(key);
		}
		
		throw new IllegalArgumentException("bad scope value");
	}
	
	private void setInScope(Scope s, String key, Object v) {
		PageContext context = (PageContext)getJspContext();
		
		switch (s) {
		case PAGE:
			context.setAttribute(key, v);
			break;
		case REQUEST:
			context.getRequest().setAttribute(key, v);
			break;
		case SESSION:
			context.getSession().setAttribute(key, v);
			break;
		case APPLICATION:
			context.getServletContext().setAttribute(key, v);
			break;
		}		
	}
	
	@Override
	public void doTag() throws IOException, JspException {
		Object current = getFromScope(scope, var);
		if (current == null)
			setInScope(scope, var, value);
	}
	
	public void setVar(String var) {
		this.var = var;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void setScope(String s) {
		if (s.equals("page"))
			scope = Scope.PAGE;
		else if (s.equals("request"))
			scope = Scope.REQUEST;
		else if (s.equals("session"))
			scope = Scope.SESSION;
		else if (s.equals("application"))
			scope = Scope.APPLICATION;
		else
			throw new IllegalArgumentException("Bad scope value " + s);
	}
}
